using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace Caucho
{
  public partial class ProgressDialog : Form
  {
    public ProgressDialog()
    {
      InitializeComponent();
      _errorProvider.Icon = SystemIcons.Error;
    }

    public delegate void SetSuccessCallBack(String success);

    public void SetSuccess(String success)
    {
      BeginInvoke(new SetSuccessCallBack(_SetSuccess), new object[] { success });
    }

    private void _SetSuccess(String success)
    {
      _timer.Stop();
      _progressBar.Value = _progressBar.Maximum;
      _statusText.Clear();
      _statusText.Text = success;
      _closeButton.Enabled = true;
    }

    public delegate void UpdateStatusCallBack(String status);

    public void UpdateStatus(String status)
    {
      BeginInvoke(new UpdateStatusCallBack(_UpdateStatus), new object[] { status });
    }

    private void _UpdateStatus(String status)
    {
      _statusText.AppendText(status);
      _statusText.AppendText("\n");
    }

    public delegate void SetErrorCallBack(String error);

    public void SetError(String error)
    {
      BeginInvoke(new SetSuccessCallBack(_SetError), new object[] { error });
    }

    private void _SetError(String error)
    {
      _timer.Stop();
      _statusText.AppendText(error);
      _errorProvider.SetError(_statusText, error);
      _closeButton.Enabled = true;
    }

    public void Reset()
    {
      _statusText.Clear();
      _closeButton.Enabled = false;
      _progressBar.Value = 0;
      _timer.Start();
    }

    public String Message
    {
      get
      {
        return null;
      }
      set
      {
        _message.Text = value;
      }
    }

    private void TimerTick(object sender, EventArgs e)
    {
      int i = _progressBar.Step * (_progressBar.Maximum - _progressBar.Value) / _progressBar.Maximum;
      _progressBar.Increment(i);
    }

    private void CloseButtonClick(object sender, EventArgs e)
    {
      Hide();
    }

    private void ProgressDialogClosing(object sender, FormClosingEventArgs e)
    {
      e.Cancel = !_closeButton.Enabled;
    }

    private void StatusTextKeyDown(object sender, KeyEventArgs e)
    {
      if (e.Modifiers.Equals(Keys.Control) && e.KeyCode.Equals(Keys.C)) {
      } else {
        e.Handled = true;
      }
    }

    private void StatusTextKeyPress(object sender, KeyPressEventArgs e)
    {
      e.Handled = true;
    }

    private void ProgressDialogFormClosed(object sender, FormClosedEventArgs e)
    {
      Reset();
    }
  }
}
