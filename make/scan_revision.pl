#!perl

$uname_machine = `uname -m`;
chop($uname_machine);

$ext = "";

if ($uname_machine) {
  $ext = "-" . $uname_machine;
}  

$uname = `uname`;
chop($uname);

if ($uname =~ "Darwin") {
  print "macosx$ext";
  exit;
}
elsif ($uname =~ "Linux") {
}
else {
  print "generic$ext";
  exit;
}

$file = "/etc/redhat-release";

if (open(file,"$file")) {
  while ($line = <file>) {
    @values = split(/\s+/, $line);
    print "rh-" . @values[2] . $ext;
    exit;
  }
}

$debian = `lsb_release --release --short 2> /dev/null`;

chop($debian);

if ($debian) {
  print "debian-$debian$ext";
  exit;
}

print "linux$ext";
exit;

