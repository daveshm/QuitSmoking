package example.naoki.ble_myo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Contacts extends ActionBarActivity {
    public static final String EXTRA_MESSAGE = "com.example.naoki.ble_myo.MESSAGE";
    public static String phonenumber = "com.example.naoki.ble_myo.MESSAGE";
    public static String filename = "phonenumber.txt";
    @Override


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        Intent intent = getIntent();
        String message = intent.getStringExtra(Contacts.EXTRA_MESSAGE);
        Toast.makeText(Contacts.this, "The phone number which is being alerted currently is " +readFile(Contacts.filename), Toast.LENGTH_LONG).show();
    }

    public void ContactSetter(View view) {

        // Get the Intent that started this activity and extract the string
        Intent intent = new Intent(this, Contacts.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        phonenumber = intent.getStringExtra(Contacts.EXTRA_MESSAGE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("STOP SMOKING");
        builder.setContentText("The phone number which will now be alerted is " + phonenumber);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(NotificationActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        NotificationManager NM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NM.notify(0, builder.build());



        saveFile(filename, phonenumber);

    }
    public void saveFile (String file, String text){
        try {
            FileOutputStream fos = openFileOutput(file, Context.MODE_PRIVATE);
            fos.write(text.getBytes());
            fos.close();
            Toast.makeText(Contacts.this, "Saved!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(Contacts.this, "Error Saving file!", Toast.LENGTH_SHORT).show();
        }
    }
    public String readFile (String file){
        String text = "";


        try {
            FileInputStream fis = openFileInput(file);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();
            text = new String(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(Contacts.this, "Error Reading file!", Toast.LENGTH_SHORT).show();
        }

        return text;
    }

}
