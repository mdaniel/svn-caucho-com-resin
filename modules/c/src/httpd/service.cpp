#include <windows.h>
#include <winsvc.h>
#include <stdio.h>
#include <stdarg.h>
#include "common.h"

static SERVICE_STATUS_HANDLE g_status_handle;
static SERVICE_STATUS g_status;
static char *g_name;       // name of service
static char *g_full_name; // display name of service
static char *g_class_name; // name of class
static int g_argc;
static char **g_argv;

extern FILE *err;
extern FILE *out;

extern int run_server(char *name, char *class_name, int argc, char **argv, int is_service);
extern void stop_server();


static int 
report_status(int currentState, int exitCode, int waitHint)
{
    static int checkPoint = 1;
 
    if (currentState == SERVICE_START_PENDING)
      g_status.dwControlsAccepted = 0;
    else
      g_status.dwControlsAccepted = SERVICE_ACCEPT_STOP;

    g_status.dwCurrentState = currentState;
    g_status.dwWin32ExitCode = exitCode;
    g_status.dwWaitHint = waitHint;

    if ((currentState == SERVICE_RUNNING) ||
	(currentState == SERVICE_STOPPED)) {
      g_status.dwCheckPoint = 0;
    }
    else
      g_status.dwCheckPoint = checkPoint++;

    // XXX: On failure, should add to event log
    return SetServiceStatus(g_status_handle, &g_status);
}

/*
 * Callback when the SCM calls ControlService()
 *
 * @param dwCtrlCode - type of control requested
 */
static VOID WINAPI service_ctrl(DWORD dwCtrlCode)
{
	switch(dwCtrlCode) {
        // Stop the service.
        //
        case SERVICE_CONTROL_STOP:
			report_status(SERVICE_STOP_PENDING, NO_ERROR, 0);
			quit_server();
			break;

        // Update the service status.
        //
        case SERVICE_CONTROL_INTERROGATE:
            break;

        // invalid control code
        //
        default:
            break;
    }
}

static void
service_main(int argc, char **argv)
{
	int exit_status = 0;

	g_status_handle = RegisterServiceCtrlHandler(g_name, service_ctrl);

	if (! g_status_handle) {
		log("service has no handler\n");
		return;
	}

	g_status.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
	g_status.dwServiceSpecificExitCode = 0;

	report_status(SERVICE_RUNNING, NO_ERROR, 3000);
	char **args = get_server_args(g_name, g_full_name, g_class_name, g_argc, g_argv);

	exit_status = spawn_java(args[0], args);

	log("stopping %s (status %d)\n", g_name, exit_status);
	report_status(SERVICE_STOPPED, NO_ERROR, 0);
}

int
start_service(char *name, char *full_name, char *class_name, int argc, char **argv)
{
	SERVICE_TABLE_ENTRY dispatch[] = {
		{ TEXT(name), (LPSERVICE_MAIN_FUNCTION) service_main },
		{ NULL, NULL }
	};
	int is_service = 0;

	g_name = name;
	g_full_name = full_name;
	g_class_name = class_name;

	g_argc = argc;
	g_argv = argv;

	if (argc > 1 && ! strcmp(argv[1], "-service"))
		is_service = 1;

	// Win95 doesn't allow service

	if (is_service && ! StartServiceCtrlDispatcher(dispatch))
		die("Can't start NT service %s.\n", name);

	return is_service;
}

static void
add_path(char *buf, char *path)
{
	if (! path) {
		strcat(buf, "\"\"");
		return;
	}

	buf += strlen(buf);
	*buf++ = ' ';
	*buf++ = '"';

	for (; *path; path++) {
		int ch = *path;

		if (ch == '"' || ch == '\'') {
		}
		else
			*buf++ = ch;
	}

	*buf++ = '"';
	*buf = 0;
}

/**
 * Installs Resin as a service.
 *
 * @param name service name
 * @param full_name full service name
 * @param service_args arguments to the service
 */
void 
install_service(char *name, char *full_name, char **service_args)
{
    SC_HANDLE   service;
    SC_HANDLE   manager;

    TCHAR path[4096];
	TCHAR args[4096];

    if (! GetModuleFileName(NULL, path, sizeof(path)))
      die("Can't get module executable");

	wsprintf(args, "\"%s\" -service", path);

	if (getenv("CLASSPATH")) {
		strcat(args, " -env-classpath ");
		add_path(args, getenv("CLASSPATH"));
	}

	if (getenv("JAVA_HOME")) {
		strcat(args, " -java_home ");
		add_path(args, getenv("JAVA_HOME"));
	}

	if (getenv("RESIN_HOME")) {
		strcat(args, " -resin_home ");
		add_path(args, getenv("RESIN_HOME"));
	}

	for (int i = 1; service_args[i]; i++) {
		if (! strcmp(service_args[i], "-install") || ! strcmp(service_args[i], "-remove"))
			continue;
		else if (! strcmp(service_args[i], "-install-as") || ! strcmp(service_args[i], "-remove-as")) {
			i++;
			continue;
		}

		strcat(args, " ");
		add_path(args, service_args[i]);
	}

    manager = OpenSCManager(NULL,               // machine (NULL == local)
			    NULL,               // database (NULL == default)
			    SC_MANAGER_ALL_ACCESS);   // access required

   if (! manager)
       die("Can't open service manager");

    service = CreateService(
            manager,     // manager
            name,        // service name
            full_name,   // display name
            SERVICE_ALL_ACCESS,         // desired access
            SERVICE_WIN32_OWN_PROCESS,  // service type
            SERVICE_AUTO_START,         // start type
            SERVICE_ERROR_NORMAL,       // error control type
            args,                       // service's binary
            NULL,                       // no load ordering group
            NULL,                       // no tag identifier
            NULL,                       // dependencies
            NULL,                       // LocalSystem account
            NULL);                      // no password

	// Don't automatically start the service
 	if (service)
		CloseServiceHandle(service);

    CloseServiceHandle(manager);

    if (! service)
      die("Can't install \"%s\" as an NT service", name);
}

void remove_service(char *name)
{
    SC_HANDLE service;
    SC_HANDLE manager;
    SERVICE_STATUS status;

    manager = OpenSCManager(
                        NULL,                   // machine (NULL == local)
                        NULL,                   // database (NULL == default)
                        SC_MANAGER_ALL_ACCESS   // access required
                        );
    if (! manager) {
      die("Can't open service manager");
    }

    service = OpenService(manager, name, SERVICE_ALL_ACCESS);

    if (service == NULL)
      die("Can't open service");

    // try to stop the service
    if (ControlService(service, SERVICE_CONTROL_STOP, &status)) {
		Sleep(1000);
		while (QueryServiceStatus(service, &status) &&
		       status.dwCurrentState == SERVICE_STOP_PENDING)
			Sleep(1000);
    }

    // now remove the service
    if (! DeleteService(service)) {
      CloseServiceHandle(service);
      CloseServiceHandle(manager);
      die("Can't remove %s as an NT service", name);
    }

    CloseServiceHandle(service);
    CloseServiceHandle(manager);
}
