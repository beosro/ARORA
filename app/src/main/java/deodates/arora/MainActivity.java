package deodates.arora;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    /*Flags
        mode_flag <0 = Manual, 1 = Auto>
        control_flag <0 = Cam, 1 = Car>
        autonomous_flag <0 = OFF, 1 = ON>
        */
    public int mode_flag = 0; //Manual
    public int control_flag = 0; //Cam
    public int autonomous_flag = 0;

    //Commands
    /*
        The commands are seperated by commas
         1. Start byte <ARORA>
         2. Mode <0: Manual, 1: Automatic>
         3. Control <0: Cam, 1: Car>
         4. PWM Set_speed <0 to 1>
         5. Car Forward_Backward Bit <1: Forward, 0: Stop, -1: Backward>
         6. Car_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         7. Cam Up_Down Bit <1: Up, 0: Stop, -1: Down>
         8. Cam_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         */
    public String header = "ARORA";
    public int cmd_mode = 0;
    public int control_mode = 0;
    public int set_speed = 0;
    public int car_fwd_back = 0;
    public int car_left_right = 0;
    public int cam_up_down = 0;
    public int cam_left_right = 0;

    //Communication data
    public String ip = "0.0.0.0";
    public int port = 1024;
    public String messageStr_tx;
    public String video_addr = "0.0.0.0";

    //For Prompt
    final Context context = this;
    final static String input_err_ip = "Please input IP address";
    final static String input_err_port = "Please input Port Number";

     @Override
    protected void onCreate(Bundle savedInstanceState) {

         super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*                                                                                              ////////////////////////
        //Checking if Wifi is ON
         WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
         if (wifi.isWifiEnabled()) {
             Toast.makeText(this, "Connected to Wifi: " + wifi.getConnectionInfo().getSSID(), Toast.LENGTH_SHORT).show();
         }
         else {
             Toast.makeText(this, "Please turn on WIFI", Toast.LENGTH_SHORT).show();
             finish(); //Exits if wifi not connected
         }
         */

        display_prompt();

        init_controls();

        init_video();
    }

    private void init_video() {

        VideoView videoView = (VideoView)findViewById(R.id.videoView);

        Uri UriSrc = Uri.parse(video_addr);
        if(UriSrc == null){
            Toast.makeText(MainActivity.this,
                    "Video source not defined", Toast.LENGTH_LONG).show();
        }else{
            videoView.setVideoURI(UriSrc);
            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            videoView.start();

            Toast.makeText(MainActivity.this,
                    "Video Connected to: " + video_addr,
                    Toast.LENGTH_LONG).show();
        }

    }

    private void display_prompt() {

         //get activity_prompt.xml
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.activity_prompt, null);
        AlertDialog.Builder alertDB = new AlertDialog.Builder(context);

        //set activity_prompt.xml to alertdialog builder
        alertDB.setView(promptsView);
        final EditText Text_ip = (EditText) promptsView.findViewById(R.id.Text_ip);
        final EditText Text_port = (EditText) promptsView.findViewById(R.id.Text_port);

        //For IP Address format check
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\." +
                            "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        Text_ip.setFilters(filters);

        /*                                                                                              ///////////////////////////
        Text_port.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //On click of port input
            }
        });
        */

        //Port value should be <=65535 and >=1024. Defining a filter
        class MinMaxFilter implements InputFilter {

            private int mIntMin, mIntMax;

            public MinMaxFilter(int minValue, int maxValue) {
                this.mIntMin = minValue;
                this.mIntMax = maxValue;
            }

            public MinMaxFilter(String minValue, String maxValue) {
                this.mIntMin = Integer.parseInt(minValue);
                this.mIntMax = Integer.parseInt(maxValue);
            }

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    int input = Integer.parseInt(dest.toString() + source.toString());
                    if (isInRange(mIntMin, mIntMax, input))
                        return null;
                } catch (NumberFormatException nfe) { }
                return "";
            }

            private boolean isInRange(int a, int b, int c) {
                return b > a ? c >= a && c <= b : c >= b && c <= a;
            }
        }
        Text_port.setFilters(new InputFilter[]{ new MinMaxFilter(0, 65535)});

        //set dialog message
        alertDB.setCancelable(false).setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                /*
                //Some algorithm to be implemented if input is null                                     //////////////////////////
                if(Text_ip.getText().length() == 0)
                {
                    //Text_ip.setError(input_err_ip);
                }
                else if(Text_port.getText().length() == 0)
                {
                    //Text_ip.setError(input_err_port);
                }
                */

                if(Text_ip.getText().toString().length() != 0)
                {
                    ip = Text_ip.getText().toString();
                }

                if(Text_port.getText().toString().length() != 0)
                {
                    port = Integer.parseInt(Text_port.getText().toString());
                }

                dialog.cancel();

            }
        })
        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish(); //exit app
            }
        });

        //create alert dialog and show
        AlertDialog aDialog = alertDB.create();
        aDialog.show();

    }

    public void init_controls() {

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        Switch switch_mode = (Switch) findViewById(R.id.switch_mode);
        final Switch switch_car_cam = (Switch) findViewById(R.id.switch_car_cam);
        TextView textView_manual = (TextView) findViewById(R.id.textView_manual);
        TextView textView_auto = (TextView) findViewById(R.id.textView_auto);
        final TextView textView_car = (TextView) findViewById(R.id.textView_car);
        final TextView textView_cam = (TextView) findViewById(R.id.textView_cam);
        final ToggleButton toggle_on_off = (ToggleButton) findViewById(R.id.toggle_on_off);

        joystick.setFixedCenter(false);
        toggle_on_off.setEnabled(false);
        toggle_on_off.setVisibility(View.INVISIBLE);
        toggle_on_off.setTextOn("STOP");
        toggle_on_off.setTextOff("START");
        switch_car_cam.setEnabled(true);
        switch_car_cam.setVisibility(View.VISIBLE);
        switch_mode.setChecked(false);
        switch_car_cam.setChecked(false);

        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                messageStr_tx = get_joystick_data(angle, strength);
                Communication(messageStr_tx);
            }
        });

        switch_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // If the switch button is on ie Auto
                    switch_car_cam.setEnabled(false);
                    switch_car_cam.setVisibility(View.INVISIBLE);
                    textView_car.setEnabled(false);
                    textView_car.setVisibility(View.INVISIBLE);
                    textView_cam.setEnabled(false);
                    textView_cam.setVisibility(View.INVISIBLE);
                    toggle_on_off.setEnabled(true);
                    toggle_on_off.setChecked(false);
                    toggle_on_off.setVisibility(View.VISIBLE);
                    mode_flag = 1; // Auto
                    control_flag = 0; // Force Cam control
                    autonomous_flag = 0;

                } else {
                    // If the switch button is off ie manual
                    toggle_on_off.setChecked(false);
                    toggle_on_off.setEnabled(false);
                    toggle_on_off.setVisibility(View.INVISIBLE);
                    switch_car_cam.setChecked(false);
                    switch_car_cam.setEnabled(true);
                    switch_car_cam.setVisibility(View.VISIBLE);
                    textView_car.setEnabled(true);
                    textView_car.setVisibility(View.VISIBLE);
                    textView_cam.setEnabled(true);
                    textView_cam.setVisibility(View.VISIBLE);
                    mode_flag = 0; // Manual
                    control_flag = 0; // Force Cam control
                    autonomous_flag = 0;
                }
            }

        });

        switch_car_cam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // If the switch button is on ie Car
                    control_flag = 1; // Car control

                } else {
                    // If the switch button is off ie cam
                    control_flag = 0; // Cam control
                }
            }

        });

        toggle_on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    autonomous_flag = 1;
                } else {
                    // The toggle is disabled
                    autonomous_flag = 0;
                }
            }
        });

    }

    public void set_mode_using_flags()
    {
        /*Flags and controls
        mode_flag <0 = Manual, 1 = Auto>
        control_flag <0 = Cam, 1 = Car>
        autonomous_flag <0 = OFF, 1 = ON>

        cmd_mode <0 = Manual, 1 = Auto>
        control_mode <0 = Cam, 1 = Car>
        */
        if (mode_flag == 0) { // if manual

            cmd_mode = 0;
        }
        else { //if automatic

            if (autonomous_flag == 0) //if autonomous OFF
            {
                cmd_mode = 0;
            }
            else{ //if autonomous ON
                cmd_mode = 1;
            }

        }

        if (control_flag == 0) { // if Cam

            control_mode = 0;
        }
        else { // if car

            control_mode = 1;

        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent i;
        switch (item.getItemId())
        {
            case R.id.menuitem_about:
                i = new Intent(this, About_Activity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {
            new AlertDialog.Builder(this).setMessage("ARORA will be disconnected").setTitle("Exit?").setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    finish();
                }
            }).setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton)
                { }
            }).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //Read the command variables and join them here
    public String get_joystick_data(int angle, int strength)
    {
        /*
        The angle starts from 0 degrees right and reads counter-clockwise
        The strength is measured from the joystick center starting from 0 and ending
        at 100 at the joystick boundary
         */

        /*
        Definition of Angle and strength
            Forward         =   72  - 108
            Forward Left    =   109 - 161
            Left            =   162 - 198
            Backward Left   =   199 - 251
            Backward        =   252 - 288
            Backward Right  =   289 - 341
            Right           =   342 - 18
            Forward Right   =   19  - 71
            set_speed %     =   strength
         */

        if (strength == 0)
        {
            car_fwd_back = 0;
            car_left_right = 0;
            cam_up_down = 0;
            cam_left_right = 0;
            set_speed = 0;
        }
        else
        {
            if (angle >= 72 && angle <= 108)
            {
                car_fwd_back = 1;
                car_left_right = 0;
                cam_up_down = 1;
                cam_left_right = 0;
                set_speed = strength;
            }
            else if (angle >= 109 && angle <= 161)
            {
                car_fwd_back = 1;
                car_left_right = -1;
                cam_up_down = 1;
                cam_left_right = -1;
                set_speed = strength;
            }
            else if (angle >= 162 && angle <= 198)
            {
                car_fwd_back = 0;
                car_left_right = -1;
                cam_up_down = 0;
                cam_left_right = -1;
                set_speed = strength;
            }
            else if (angle >= 199 && angle <= 251)
            {
                car_fwd_back = -1;
                car_left_right = -1;
                cam_up_down = -1;
                cam_left_right = -1;
                set_speed = strength;
            }
            else if (angle >= 252 && angle <= 288)
            {
                car_fwd_back = -1;
                car_left_right = 0;
                cam_up_down = -1;
                cam_left_right = 0;
                set_speed = strength;
            }
            else if (angle >= 289 && angle <= 341)
            {
                car_fwd_back = -1;
                car_left_right = 1;
                cam_up_down = -1;
                cam_left_right = 1;
                set_speed = strength;
            }
            else if ((angle >= 342 && angle <= 359) || (angle >= 0 && angle <= 18) )
            {
                car_fwd_back = 0;
                car_left_right = 1;
                cam_up_down = 0;
                cam_left_right = 1;
                set_speed = strength;
            }
            else if (angle >= 19 && angle <= 71)
            {
                car_fwd_back = 1;
                car_left_right = 1;
                cam_up_down = 1;
                cam_left_right = 1;
                set_speed = strength;
            }
            else
            {
                car_fwd_back = 0;
                car_left_right = 0;
                cam_up_down = 0;
                cam_left_right = 0;
                set_speed = strength;
            }
        }

        set_mode_using_flags();

        /*
        The commands are seperated by commas
         1. Start byte <ARORA>
         2. Mode <0: Manual, 1: Automatic>
         3. Control <0: Cam, 1: Car>
         4. PWM Set_speed <0 to 1>
         5. Car Forward_Backward Bit <1: Forward, 0: Stop, -1: Backward>
         6. Car_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         7. Cam Up_Down Bit <1: Up, 0: Stop, -1: Down>
         8. Cam_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         */

        List<String> cmds = new ArrayList<String>();
        cmds.add(header);
        cmds.add(Integer.toString(cmd_mode));
        cmds.add(Integer.toString(control_mode));
        cmds.add(Integer.toString(set_speed));
        cmds.add(Integer.toString(car_fwd_back));
        cmds.add(Integer.toString(car_left_right));
        cmds.add(Integer.toString(cam_up_down));
        cmds.add(Integer.toString(cam_left_right));
        String messageStr_tx = TextUtils.join(",", cmds);
        return messageStr_tx;
    }

    public void Communication(String messageStr_tx)
    {
        boolean run = true;
        try {

            // Transmission
            DatagramSocket udpSocket = new DatagramSocket(port);
            InetAddress serverAddr = InetAddress.getByName(ip);                                         //////////////////////////

            byte[] message_tx = messageStr_tx.getBytes();

            DatagramPacket packet_tx = new DatagramPacket(message_tx, message_tx.length, serverAddr, port);
            udpSocket.send(packet_tx);

            try {

                    //Reception
                    byte[] message_rx = new byte[8000];
                    DatagramPacket packet_rx = new DatagramPacket(message_rx, message_rx.length);
                    udpSocket.setSoTimeout(10000);
                    udpSocket.receive(packet_rx);

                    String messageStr_rx = new String(message_rx, 0, packet_rx.getLength());
                    //Call read_message fn of Command_Class here and pass the messageStr_rx to it

                } catch (IOException e) {
                    Log.e(" UDP client has IOExc", "error: ", e);
                    run = false;
                    udpSocket.close();
                }

        } catch (SocketException e) {
            Log.e("Socket Open:", "Error:", e);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
