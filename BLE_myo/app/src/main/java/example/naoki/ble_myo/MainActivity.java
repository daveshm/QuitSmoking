package example.naoki.ble_myo;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Queue;
import java.util.LinkedList;

public class MainActivity extends ActionBarActivity implements BluetoothAdapter.LeScanCallback {
    public static final int MENU_LIST = 0;
    public static final int MENU_BYE = 1;



    /** Device Scanning Time (ms) */
    private static final long SCAN_PERIOD = 5000;

    /** Intent code for requesting Bluetooth enable */
    private static final int REQUEST_ENABLE_BT = 1;

    private static final String TAG = "BLE_Myo";

    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt    mBluetoothGatt;
    private TextView         emgDataText;
    private TextView         gestureText;

    private MyoGattCallback mMyoCallback;
    private MyoCommandList commandList = new MyoCommandList();

    private String deviceName;

    private GestureSaveModel    saveModel;
    private GestureSaveMethod   saveMethod;
    private GestureDetectModel  detectModel;
    private GestureDetectMethod detectMethod;

    private LineGraph graph;
    private Button graphButton1;
    private Button graphButton2;
    private Button graphButton3;
    private Button graphButton4;
    private Button graphButton5;
    private Button graphButton6;
    private Button graphButton7;
    private Button graphButton8;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);







        //ready
        graph = (LineGraph) findViewById(R.id.holo_graph_view);
        graphButton1 = (Button) findViewById(R.id.btn_emg1);
        graphButton2 = (Button) findViewById(R.id.btn_emg2);
        graphButton3 = (Button) findViewById(R.id.btn_emg3);
        graphButton4 = (Button) findViewById(R.id.btn_emg4);
        graphButton5 = (Button) findViewById(R.id.btn_emg5);
        graphButton6 = (Button) findViewById(R.id.btn_emg6);
        graphButton7 = (Button) findViewById(R.id.btn_emg7);
        graphButton8 = (Button) findViewById(R.id.btn_emg8);


        //set color
        graphButton1.setBackgroundColor(Color.argb(0x66, 0xff, 0, 0xff));
        graphButton2.setBackgroundColor(Color.argb(0x66, 0xff, 0x00, 0x00));
        graphButton3.setBackgroundColor(Color.argb(0x66, 0x66, 0x33, 0xff));
        graphButton4.setBackgroundColor(Color.argb(0x66, 0xff, 0x66, 0x33));
        graphButton5.setBackgroundColor(Color.argb(0x66, 0xff, 0x33, 0x66));
        graphButton6.setBackgroundColor(Color.argb(0x66, 0x00, 0x33, 0xff));
        graphButton7.setBackgroundColor(Color.argb(0x66, 0x00, 0x33, 0x33));
        graphButton8.setBackgroundColor(Color.argb(0x66, 0x66, 0xcc, 0x66));

        emgDataText = (TextView)findViewById(R.id.emgDataTextView);
        gestureText = (TextView)findViewById(R.id.gestureTextView);
        mHandler = new Handler();

        startNopModel();

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        Intent intent = getIntent();
        deviceName = intent.getStringExtra(ListActivity.TAG);

        if (deviceName != null) {
            // Ensures Bluetooth is available on the device and it is enabled. If not,
            // displays a dialog requesting user permission to enable Bluetooth.
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // Scanning Time out by Handler.
                // The device scanning needs high energy.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.stopLeScan(MainActivity.this);
                    }
                }, SCAN_PERIOD);
                mBluetoothAdapter.startLeScan(this);
            }
        }
        Toast.makeText(MainActivity.this, "The number of times you've been caught is " +counter(), Toast.LENGTH_LONG).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(0, MENU_LIST, 0, "Find Myo");
        menu.add(0, MENU_BYE, 0, "Good Bye");
        return true;
    }

    @Override
    public void onStop(){
        super.onStop();
        this.closeBLEGatt();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch (id) {
            case MENU_LIST:
//                Log.d("Menu","Select Menu A");
                Intent intent = new Intent(this,ListActivity.class);
                startActivity(intent);
                return true;

            case MENU_BYE:
//                Log.d("Menu","Select Menu B");
                closeBLEGatt();
                Toast.makeText(getApplicationContext(), "Close GATT", Toast.LENGTH_SHORT).show();
                startNopModel();
                return true;

        }
        return false;
    }

    /** Define of BLE Callback */
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (deviceName.equals(device.getName())) {
            mBluetoothAdapter.stopLeScan(this);
            // Trying to connect GATT
            HashMap<String,View> views = new HashMap<String,View>();
            //put GraphView
            views.put("graph",graph);
            //put Button1〜8

            views.put("btn1",graphButton1);
            views.put("btn2",graphButton2);
            views.put("btn3",graphButton3);
            views.put("btn4",graphButton4);
            views.put("btn5",graphButton5);
            views.put("btn6",graphButton6);
            views.put("btn7",graphButton7);
            views.put("btn8",graphButton8);

            mMyoCallback = new MyoGattCallback(mHandler, emgDataText, views);
            mBluetoothGatt = device.connectGatt(this, false, mMyoCallback);
            mMyoCallback.setBluetoothGatt(mBluetoothGatt);
        }
    }

    public void onClickVibration(View v){
        if (mBluetoothGatt == null || !mMyoCallback.setMyoControlCommand(commandList.sendVibration3())) {
            Log.d(TAG, "False Vibrate");
        }
    }

    public void onClickUnlock(View v) {
        if (mBluetoothGatt == null || !mMyoCallback.setMyoControlCommand(commandList.sendUnLock())) {
            Log.d(TAG,"False UnLock");
        }
    }

    public void onClickEMG(View v) {
        if (mBluetoothGatt == null || !mMyoCallback.setMyoControlCommand(commandList.sendEmgOnly())) {
            Log.d(TAG,"False EMG");
        } else {
            saveMethod  = new GestureSaveMethod();
            if (saveMethod.getSaveState() == GestureSaveMethod.SaveState.Have_Saved) {
                gestureText.setText("DETECT Ready");
            } else {
                gestureText.setText("Teach me \'Gesture\'");
            }
        }
    }

    public void onClickNoEMG(View v) {
        if (mBluetoothGatt == null
                || !mMyoCallback.setMyoControlCommand(commandList.sendUnsetData())
                || !mMyoCallback.setMyoControlCommand(commandList.sendNormalSleep())) {
            Log.d(TAG,"False Data Stop");
        }
    }

    public void onClickSave(View v) {
        if (saveMethod.getSaveState() == GestureSaveMethod.SaveState.Ready ||
                saveMethod.getSaveState() == GestureSaveMethod.SaveState.Have_Saved) {
            saveModel   = new GestureSaveModel(saveMethod);
            startSaveModel();
        } else if (saveMethod.getSaveState() == GestureSaveMethod.SaveState.Not_Saved) {
            startSaveModel();
        }
        saveMethod.setState(GestureSaveMethod.SaveState.Now_Saving);
        gestureText.setText("Saving ; " + (saveMethod.getGestureCounter() + 1));
    }

    public void onClickDetect(View v) {
        if (saveMethod.getSaveState() == GestureSaveMethod.SaveState.Have_Saved) {
            gestureText.setText("Let's Go !!");
            detectMethod = new GestureDetectMethod(saveMethod.getCompareDataList());
            detectModel = new GestureDetectModel(detectMethod);
            startDetectModel();
        }
    }

    public void closeBLEGatt() {
        if (mBluetoothGatt == null) {
            return;
        }
        mMyoCallback.stopCallback();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void startSaveModel() {
        IGestureDetectModel model = saveModel;
        model.setAction(new GestureDetectSendResultAction(this));
        GestureDetectModelManager.setCurrentModel(model);
    }

    public void startDetectModel() {
        IGestureDetectModel model = detectModel;
        model.setAction(new GestureDetectSendResultAction(this));
        GestureDetectModelManager.setCurrentModel(model);
    }

    public void startNopModel() {
        GestureDetectModelManager.setCurrentModel(new NopModel());
    }

    public static boolean isTime = false;

    public void setGestureText(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                gestureText.setText(message);
            }
        });


        if(message == "Gesture_1" || message == "Gesture_2" || message == "Gesture_3"){
            if(isTime == true){

                return;

            }
            startBomb(findViewById(android.R.id.content));
        }
    }

    public void messageTimer(){

        TimerTask task = new TimerTask() {
            public void run() {
               isTime = false;
            }
        };
        Timer timer = new Timer("Timer");

        long delay = 10000;


        timer.schedule(task, delay);//10 Min


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(MainActivity.this);
                }
            }, SCAN_PERIOD);
            mBluetoothAdapter.startLeScan(this);
        }
    }
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public static String phonenumber = Contacts.phonenumber;


    /**
     * Called when the user taps the Send button
     */
    public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);



    }


    public void showNotification(View view) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("STOP SMOKING");
        builder.setContentText(facts());
        Intent intent = new Intent(this, NotificationActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(NotificationActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        NotificationManager NM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NM.notify(0, builder.build());


        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
    }



    public void startBomb(View view) {

        System.out.println("reached here!");

        // Initialise smsManager
        SmsManager smsManager = SmsManager.getDefault();


        smsManager.sendTextMessage(readFile(Contacts.filename), null, "A loved one has been caught smoking. Here's why they should stop: " +facts() , null, null);

        showNotification(view);

        counter();

        isTime = true;
        messageTimer();


    }



    public void contacts(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, Contacts.class);
        //EditText editText = (EditText) findViewById(R.id.editText);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void saveFile(String file, String text){
        try{
            FileOutputStream fos = openFileOutput(file, Context.MODE_PRIVATE);
            fos.write(text.getBytes());
            fos.close();
            //Toast.makeText(MainActivity.this, "Saved!", Toast.LENGTH_SHORT).show();

        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error Saving file!", Toast.LENGTH_SHORT).show();
        }
    }

    public String readFile(String file){
        String text = "";



        try {
            FileInputStream fis = openFileInput(file);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();
            text = new String(buffer);
            //Toast.makeText(MainActivity.this, "READ!", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error Reading file!", Toast.LENGTH_SHORT).show();
        }

        return text;
    }

    public static String facts () {


        String[] facts=new String[151];

        facts[0]="The chemicals in tobacco smoke Contribute to inflammation, which may trigger plaque buildup in your arteries. This buildup is called atherosclerosis.";
        facts[1]="The chemicals in tobacco smoke Contribute to inflammation, which may trigger plaque buildup in your arteries. This buildup is called atherosclerosis.";
        facts[2]="The chemicals in tobacco smoke disturb normal heart rhythms.";
        facts[3]="Although smoking temporarily puts the smoker in a good mood, once the initial rush fades, the smoker is left feeling anxious and irritable.";
        facts[4]="Smoking results in reduced blood flow which causes your skin to get less nutrition.";
        facts[5]="When compared to people who don’t smoke, people who do smoke have an increased risk of blood cancer, such as leukemia.";
        facts[6]="Handling tobacco products can stain your fingers and fingernails, turning them yellow.";
        facts[7]="The chemicals in tobacco smoke damage blood vessel walls, making them stiff and less elastic (stretchy). This damage narrows the blood vessels and contributes to the damage caused by unhealthy cholesterol levels.";
        facts[8]="Smoking raises blood pressure.";
        facts[9]="Smokers experience weakened blood vessel walls.";
        facts[10]="Smoking increases blood clots.";
        facts[11]="Smoking not only impacts your cardiovascular health, but also the health of those around you who don’t smoke, putting them at risk.";
        facts[12]="Secondhand smoking puts loved ones at risk of strokes, heart disease, and heart attacks.";
        facts[13]="The chemicals in tobacco smoke lower your HDL (“good”) cholesterol and raise your LDL (“bad”) cholesterol.";
        facts[14]="Smoking increases the likelihood of fungal nail infections.";
        facts[15]="Smoking increases hair loss.";
        facts[16]="The chemicals in tobacco smoke increase your blood pressure and heart rate, making your heart work harder than normal.";
        facts[17]="Smoking causes balding.";
        facts[18]="Smoking causes graying hair.";
        facts[19]="The chemicals in tobacco smoke thicken your blood and make it harder for your blood to carry oxygen.";
        facts[20]="Even people who “smoke but don’t inhale” face an increased risk of mouth cancer.";
        facts[21]="Each day 1300 people die from smoking alone.";
        facts[22]="Smoking can make it more difficult for blood to move around in the body, so many smokers feel tired and cranky.";
        facts[23]="A pack of cigarettes costs about $6, on average. That means, even if you buy just one pack a week, you'll spend $312 in a year. Some people smoke a pack a day, which adds up to $2,190.";
        facts[24]="5.6 million children alive today will ultimately die early from smoking. That is equal to 1 child out of every 13 alive in the U.S. today.";
        facts[25]="Substances in cigarettes can cause dry skin and wrinkles.";
        facts[26]="An estimated 88 million nonsmoking Americans, including 54% of children aged 3–11 years, are exposed to secondhand smoke.";
        facts[27]="Each year, primarily because of exposure to secondhand smoke, an estimated 3,000 nonsmoking Americans die of lung cancer.";
        facts[28]="Nonsmokers who are exposed to secondhand smoke at home or work increase their lung cancer risk by 20–30%.";
        facts[29]="Nonsmokers who are exposed to secondhand smoke at home or work increase their heart disease risk by 25–30%.";
        facts[30]="More than 33,000 nonsmokers die every year in the United States from coronary heart disease caused by exposure to secondhand smoke.";
        facts[31]="There are around 1.2 billion smokers in the world (about one-third of the global population aged 15 and over).";
        facts[32]="At least 4.5 trillion [non-biodegradable] filter-tipped cigarettes are deposited annually somewhere in the world. These have devastating effects on the environment.";
        facts[33]="Each year nearly 600 million trees are destroyed to provide fuel to dry tobacco. Put in another way one tree is destroyed for every 300 cigarettes.";
        facts[34]="Globally, tobacco curing requires 11.4 million tons of solid wood annually.";
        facts[35]="Tobacco is particularly potassium-hungry, absorbing up to six times as much as other crops, leaving soil in poor condition for essential food and cash crops.";
        facts[36]="Modern cigarette manufacturing machines use more than six kilometres of paper per hour. Taking into account the wastage of paper as cigarettes are used and discarded, cigarettes contribute to pollution.";
        facts[37]="In 1995 worldwide tobacco manufacturing produced 2.26 billion kilograms of solid waste and 209 million kilograms of chemical waste. As people continue to smoke, these numbers have only increased.";
        facts[38]="Tobacco is the second major cause of death in the world. It is currently responsible for the death of one in ten adults worldwide (about 5 million deaths each year).";
        facts[39]="Half the people that smoke today—that is about 650 million people—will eventually be killed by tobacco.";
        facts[40]="Cigarette smoke contains polonium 210, a radioactive element. One study shows that a person who smokes 20 cigarettes a day receives a dose of radiation each year equivalent to about 200 chest x-rays.";
        facts[41]="Smoking causes 1 in 5 deaths in the U.S. every year.";
        facts[42]="A single cigarette contains over 4,800 chemicals, 69 of which are known to cause cancer.";
        facts[43]="Every day, nearly 4,000 teens in the U.S. smoke their first cigarette, while 1,000 start smoking on a daily basis.";
        facts[44]="The average smoker in the U.S. spends $1,500 to $3,300 a year to feed their addiction.";
        facts[45]="Smoking costs the U.S. $333 billion per year in health care expenses and lost productivity to boot.";
        facts[46]="Exposure to secondhand smoke causes nearly 50,000 deaths each year in the U.S. alone.";
        facts[47]="15 billion cigarettes are smoked worldwide every day.";
        facts[48]="About 69% of smokers want to quit completely.";
        facts[49]="Over 30% of cancer could be prevented by avoiding tobacco and alcohol, having a healthy diet and physical activity.";
        facts[50]="8.6 million people in the U.S. live with a serious illness caused by smoking.";
        facts[51]="Smoking makes the risk of a heart attack 200% to 400% greater than that of nonsmokers.";
        facts[52]="Urea, a main component in urine, is added to cigarettes to enhance their flavor.";
        facts[53]="Within 48 hours of quitting smoking, your nerve endings begin to regrow and your senses of smell and taste begin to return to normal.";
        facts[54]="Smoking a cigarette causes damage in minutes, not years.";
        facts[55]="There is enough nicotine in four or five cigarettes to kill an average adult if ingested whole.";
        facts[56]="Ambergris, or whale dung, is one of the hundreds of possible additives used in manufactured cigarettes.";
        facts[57]="Benzene is a known cause of acute myeloid leukemia, and cigarette smoke is a major source of benzene exposure.";
        facts[58]="Radioactive lead and polonium are both present in low levels in cigarette smoke.";
        facts[59]="Hydrogen cyanide, one of the toxic byproducts present in cigarette smoke, was used as a genocidal chemical agent during World War II.";
        facts[60]="Secondhand smoke contains more than 70 cancer-causing chemical compounds.";
        facts[61]="Statistics tell us that 5.6 million children alive today in the U.S. will die of a smoking-related disease. That is equal to 1 in 13 kids living in the U.S. today.";
        facts[62]="Before it kills us, tobacco usually offers us plenty of suffering. Approximately 16 million Americans are living with a tobacco-related disease right now. Or put another way, for every death, 30 people live with a disease caused by tobacco.";
        facts[63]="Half of all long-term smokers will die a tobacco-related death.";
        facts[64]="Every 5 seconds, a human life is lost to tobacco use somewhere in the world.";
        facts[65]="Tobacco use claimed 100 million lives during the 20th century around the world. It is expected to claim one billion lives during the 21st century unless serious anti-smoking efforts are made on a global level.";
        facts[66]="About half of all Americans who keep smoking will die because of the habit.";
        facts[67]="Each year more than 480,000 people in the United States die from illnesses related to tobacco use.";
        facts[68]="Each year smoking causes about 1 out of 5 deaths in the US.";
        facts[69]="Smoking cigarettes kills more Americans than alcohol, car accidents, HIV, guns, and illegal drugs combined.";
        facts[70]="Cigarette smokers die younger than non-smokers.";
        facts[71]="Smoking shortens male smokers’ lives by about 12 years and female smokers’ lives by about 11 years.";
        facts[72]="Smoking accounts for about 30% of all cancer deaths in the United States, including about 80% of all lung cancer deaths.";
        facts[73]="Smoking makes pneumonia and asthma worse. It also causes many other lung diseases that can be nearly as bad as lung cancer.";
        facts[74]="COPD, or chronic obstructive pulmonary disease, is the name for long-term lung disease which includes both chronic bronchitis and emphysema. It makes it extremely hard to breathe, and there is no cure. COPD is the third leading cause of death in the United States.";
        facts[75]="Smoking lowers the body’s ability to heal, so wounds take longer to clot.";
        facts[76]="Smokers experience a decreased sense of smell and taste.";
        facts[77]="Smokers experience premature aging of the skin.";
        facts[78]="Smokers face an increased risk of cataracts (clouding of the lenses of the eyes).";
        facts[79]="Smokers develop bad breath and stained teeth.";
        facts[80]="Smokers develop lower bone density (thinner bones), which means they are at a higher risk of broken bones.";
        facts[81]="There is a decrease in immune system function in smokers.";
        facts[82]="Smokers are at a higher risk of developing rheumatoid arthritis.";
        facts[83]="There is an increased risk of blindness in smokers.";
        facts[84]="Smokers face an increased risk of ulcers.";
        facts[85]="Smokers often face coughing spells.";
        facts[86]="Smokers often experience shortness of breath, even when not exercising. Wheezing and gasping is common.";
        facts[87]="Smokers are more likely than nonsmokers to develop heart disease, stroke, and lung cancer.";
        facts[88]="Smoking causes stroke and coronary heart disease, which are among the leading causes of death in the United States.";
        facts[89]="Smokers are at greater risk for diseases that affect the heart and blood vessels (cardiovascular disease).";
        facts[90]="Smoking can cause lung disease by damaging your airways and the small air sacs (alveoli) found in your lungs.";
        facts[91]="Cigarette smoking causes most cases of lung cancer.";
        facts[92]="If you have asthma, tobacco smoke can trigger an attack or make an attack worse.";
        facts[93]="If nobody smoked, one of every three cancer deaths in the United States would not happen.";
        facts[94]="Smoking increases the risk of dying from cancer and other diseases in cancer patients and survivors.";
        facts[95]="Smoking can cause cancer almost anywhere in your body.";
        facts[96]="Smoking harms nearly every organ of the body and affects a person’s overall health.";
        facts[97]="Smoking affects the health of your teeth and gums and can cause tooth loss.";
        facts[98]="Smoking can increase your risk for cataracts (clouding of the eye’s lens that makes it hard for you to see).";
        facts[99]="Smoking is a cause of type 2 diabetes mellitus and can make it harder to control. The risk of developing diabetes is 30–40% higher for active smokers than nonsmokers.";
        facts[100]="Smoking causes general adverse effects on the body, including inflammation and decreased immune function.";
        facts[101]="Quitting smoking cuts cardiovascular risks. Just 1 year after quitting smoking, your risk for a heart attack drops sharply.";
        facts[102]="If you quit smoking, your risks for cancers of the mouth, throat, esophagus, and bladder drop by half within 5 years.";
        facts[103]="Within 2 to 5 years after quitting smoking, your risk for stroke may reduce to about that of a nonsmoker’s.";
        facts[104]="Cigarette smoking causes bone loss.";
        facts[105]="Cigarette smoking results in digestive disorders.";
        facts[106]="Cigarette smoke ruins clothing, furniture, and car seats.";
        facts[107]="Smoking damages family and social relationships.";
        facts[108]="Smoking-related diseases generate more than $50 billion a year in medical costs.";
        facts[109]="Lost wages and lost productivity from smoking-related diseases cost another $50 billion a year.";
        facts[110]="Smoking, or inhaling secondhand smoke during pregnancy puts babies at risk for low birth weight, premature death, and sudden infant death syndrome, as well as learning disabilities.";
        facts[111]="More than 6,200 children die each year from infections and burns because of parents who smoke.";
        facts[112]="Cigarette smoking is a major cause of fire-related deaths.";
        facts[113]="Matches and lighters are a major cause of house fires.";
        facts[114]="Each day, more than 5,000 children try smoking, and 3,000 become hooked.";
        facts[115]="Immediately after quitting smoking, your body’s healing processes begin.";
        facts[116]="20 minutes after you take your final cigarette, your blood pressure lowers.";
        facts[117]="20 minutes after your last cigarette, your hands and feet warm up.";
        facts[118]="Eight hours after your last cigarette, the carbon monoxide level in your blood returns to normal.";
        facts[119]="One day after your final cigarette, your heart attack risk decreases.";
        facts[120]="One day after your final cigarette, your ability to breathe increases.";
        facts[121]="Three days after your last cigarette, your sense of taste and smell improve.";
        facts[122]="Three days after your final cigarette, your skin begins to look and feel better.";
        facts[123]="Your energy levels increase three days after your last cigarette.";
        facts[124]="Your mood improves about one week after your final cigarette.";
        facts[125]="You become less irritable one week after your last cigarette.";
        facts[126]="Two weeks after your last cigarette, your circulation improves.";
        facts[127]="Your lung function increases two weeks after you quit smoking.";
        facts[128]="One to nine months after your quit smoking, your smoker’s cough decreases, and your lungs’ cleansing function returns to normal.";
        facts[129]="Your risk for infection decreases one to nine months after you quit smoking.";
        facts[130]="One year after your last cigarette, your heart attack risk is half that of a smoker.";
        facts[131]="You’ve saved $2,190 or more from not buying cigarettes one year after you quit smoking.";
        facts[132]="Five to fifteen years after quitting, your stroke risk is equal to that of a non-smoker.";
        facts[133]="10 years after quitting smoking, your lung cancer risk is half that of a smoker.";
        facts[134]="Your risk of cancer decreases 10 years after smoking your last cigarette.";
        facts[135]="15 years after quitting smoking, your risk of heart disease is equal to that of a nonsmoker.";
        facts[136]="A cigarette contains more than 7,000 chemicals in it. Out of these, several hundred are toxic and 70 of them can cause cancer.";
        facts[137]="Some of the most harmful ingredients of a cigarette contains lead, radon, tar, radium, carbon monoxide, ammonia, hydrogen cyanide, and uranium.";
        facts[138]="According to British scientists, every cigarette you smoke can shorten your life by 11 minutes.";
        facts[139]="Smoking makes you paler.";
        facts[140]="Due to smoking, the supply of blood to your skin is reduced, which lowers the level of Vitamin A in your body.";
        facts[141]="Occasional smoking is harmful. Even relatively small amounts [of tobacco] damage your blood vessels and make your blood more likely to clot, causing heart attacks, strokes, and even sudden death.";
        facts[142]="Contrary to popular belief, ‘light’ cigarettes are not safer. You can get just as much tar smoking a light cigarette as a regular one.";
        facts[143]="It’s never too late to quit smoking. Even if you’ve smoked your whole life, it’s worth it to stop. Right away, your heart rate and blood pressure will go down, and your lungs will start to work better.";
        facts[144]="E-cigarettes are not a healthy alternative. The U.S. surgeon general found that the aerosol in e-cigarettes can contain damaging chemicals, including nicotine, ultrafine particles you can inhale into your lungs, flavorings linked to lung disease, and heavy metals.";
        facts[145]="Even if you work out, eat your fruits and vegetables, and otherwise take care of yourself, it’s still not okay to smoke. There is no research that shows that exercise or diet can undo the negative impact of smoking.";
        facts[146]="Your kids will be less likely to start smoking if you quit.";
        facts[147]="Quitting smoking already puts you in better control of your life.";
        facts[148]="You will reduce the health risks for your family members caused by secondhand smoke when you quit smoking.";
        facts[149]="Quitting smoking will greatly increase your quality of life.";
        facts[150]="More than 400,000 babies born in the U.S. every year are exposed to chemicals in cigarette smoke before birth, because their mothers smoke.";


        Random rn = new Random();
        int range = facts.length - 1;
        int randomNum =  rn.nextInt(range);

        return facts[randomNum];
    }



    public int counter(){


        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        int tempN=pref.getInt("N", 0);
        tempN++;
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("N",tempN);

        editor.commit();

        return tempN;




    }

}


