package onscreen;

/**
 *
 * @author Mattias
 */
import java.io.BufferedInputStream;
import java.io.InputStream;

import java.io.OutputStream;
import javax.microedition.io.StreamConnection;

public class ProcessConnectionThread implements Runnable {

    private StreamConnection mConnection;
    private FileReciver fileReciver;
    private static final int EXIT_CMD = 10;
    private static final int FILE = 1;
    private static final int MOUSECONTROLLER = 2;
    private static final int REQUESTCONTROL = 3;
    private static final int RELEASECONTROL = 4;
    private static final int KEYCONTROLLER = 5;
    private static ProcessConnectionThread lockOwner = null;
    
    ProcessConnectionThread(StreamConnection connection) {
        mConnection = connection;
        fileReciver = new FileReciver();
    }
    
    private synchronized boolean lockControl() {
        if (lockOwner == null) {
            lockOwner = this;
            return true;
        }
        return false;        
    }
    
    private synchronized boolean canControl() {
        if (lockOwner == this) {
            return true;
        }
        return false;
    }
    
    private synchronized void releaseControl() {
        if (canControl()) {
            lockOwner = null;
        }
    }
    
    @Override
    public void run() {
        try {
            // prepare to receive data
            InputStream inputStream = mConnection.openInputStream();
            OutputStream out = mConnection.openOutputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            
            while (true) {
                int command = bufferedInputStream.read();

                switch (command) {
                    case EXIT_CMD:
                        mConnection.close();
                        return;
                    case FILE:
                        String file = fileReciver.reciveFile(bufferedInputStream);
                        Runtime rt = Runtime.getRuntime();
                        Process pr = rt.exec(OnScreen.pdfReader + file);
                        break;
                    case MOUSECONTROLLER:
                        if (canControl()) {
                            OnScreen.mouseController.recive(bufferedInputStream);
                        } else {
                            bufferedInputStream.read();
                            bufferedInputStream.read();
                        }
                        break;
                    case KEYCONTROLLER:
                        if (canControl()) {
                            OnScreen.keyController.recive(bufferedInputStream.read());
                        } else {
                            bufferedInputStream.read();
                        }
                        break;
                    case REQUESTCONTROL:
                        if(lockControl()){
                            out.write(1);
                        } else {
                            out.write(0);
                        }
                        break;
                    case RELEASECONTROL:
                        releaseControl();
                        break;
                    default:
                        Notification.notify("Unknown control sequence " + command);
                        break;
                }
            }
        } catch (Exception e) {
            Notification.notify("Something went wrong " );
            e.printStackTrace();
        }
    }
    
    
}
