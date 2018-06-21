package deodates.arora;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import io.github.controlwear.virtual.joystick.android.JoystickView;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
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
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
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
         2. Serial Number
         3. Mode <0: Manual, 1: Automatic>
         4. Control <0: Cam, 1: Car>
         5. PWM Set_speed <0 to 100>
         6. Car Forward_Backward Bit <1: Forward, 0: Stop, -1: Backward>
         7. Car_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         8. Cam Up_Down Bit <1: Up, 0: Stop, -1: Down>
         9. Cam_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         */
    public String header = "ARORA";
    public int udp_serial_no = 0;
    public int cmd_mode = 0;
    public int control_mode = 0;
    public int set_speed = 0;
    public int car_fwd_back = 0;
    public int car_left_right = 0;
    public int cam_up_down = 0;
    public int cam_left_right = 0;

    //For Joystick data
    public int global_angle = 0;
    public int global_strength = 0;

    //Communication data
    public String ip = "10.0.0.5";
    public int udp_port = 5555;
    public String messageStr_tx = "null";
    public String messageStr_rx = "null";
    public String udp_state = "null";
    public int video_port = 8081;

    //For Video
    //VideoView videoView = (VideoView)findViewById(R.id.videoView);

    //For exo player
    //SimpleExoPlayerView exoPlayerView;
    //SimpleExoPlayer exoPlayer;

    //For UDP Handler
    UdpClientHandler udpClientHandler;
    UdpClientThread udpClientThread;

    //For Prompt
    public boolean display_prompt = true;
    final Context context = this;
    final static String input_err_ip = "Please input IP address";
    final static String input_err_port = "Please input Port Number";

     @Override
    protected void onCreate(Bundle savedInstanceState) {

         super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifi_check();

         if(display_prompt == true)
         {
             display_prompt();
         }

         create_udp_client_handler();

         init_controls();

         //init_exo_player();

        //init_video_videoview();

         init_webView();
    }

    private void init_webView() {

        WebView web_view = (WebView) findViewById(R.id.web_view);

        String video_addr = "http://" + ip + ":" + video_port;
        web_view.loadUrl(video_addr);
    }

    /*
    private void init_exo_player() {

        exoPlayerView = (SimpleExoPlayerView) findViewById(R.id.exo_player_view);
        try {
            //BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            //TrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(bandwidthMeter));
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this, null);

            String video_addr = ip + ":" + video_port;
            Uri videoURI = Uri.parse(video_addr);

            DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory("exoplayer_video");
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            MediaSource mediaSource = new ExtractorMediaSource(videoURI, dataSourceFactory, extractorsFactory, null, null);

            exoPlayerView.setPlayer(exoPlayer);
            exoPlayer.prepare(mediaSource);
            exoPlayer.setPlayWhenReady(true);
        }
        catch (Exception e){
            Log.e("MainAcvtivity"," exoplayer error "+ e.toString());
        }

    }
    */

    private void create_udp_client_handler() {

        udpClientHandler = new UdpClientHandler(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //videoView.stopPlayback();
    }

    private void wifi_check()
    {
        //Checking if Wifi is ON
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {

            //Take current SSID and formats it to normal double quote string by eliminating xtra double quotes
            String current_SSID = wifi.getConnectionInfo().getSSID().replaceAll("\"","");

            Toast.makeText(this, "Connected to Wifi: " + current_SSID, Toast.LENGTH_SHORT).show();

            String[] known_SSIDs = {"arora_AP"}; //Add more SSID's if needed

            if(Arrays.asList(known_SSIDs).contains(current_SSID))
            {
                display_prompt = false;
            }
        }
        else {
            Toast.makeText(this, "Please turn on WIFI and restart app", Toast.LENGTH_SHORT).show();
            //finish(); //Exits if wifi not connected
        }
    }

    private void updateState(String state){
        udp_state = state;
    }

    private void updateRxMsg(String rxmsg){
        messageStr_rx = rxmsg;
    }

    private void clientEnd(){
        udpClientThread = null;
    }

    public static class UdpClientHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        private MainActivity parent;

        public UdpClientHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case UPDATE_STATE:
                    parent.updateState((String)msg.obj);
                    break;
                case UPDATE_MSG:
                    parent.updateRxMsg((String)msg.obj);
                    break;
                case UPDATE_END:
                    parent.clientEnd();
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }


    /*
    public void init_video_videoview() {

        String video_addr = ip + ":" + video_port;
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

        //To disable the alert "can't play this video"
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d("video", "setOnErrorListener ");
                return true;
            }
        });

    }
    */

    private void display_prompt() {

        //get activity_prompt.xml
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.activity_prompt, null);
        AlertDialog.Builder alertDB = new AlertDialog.Builder(context);

        //set activity_prompt.xml to alertdialog builder
        alertDB.setView(promptsView);
        final EditText Text_ip = (EditText) promptsView.findViewById(R.id.Text_ip);
        final EditText Text_port = (EditText) promptsView.findViewById(R.id.Text_port);
        final EditText Text_port_vid = (EditText) promptsView.findViewById(R.id.Text_port_vid);

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

                //Some algorithm can be implemented if input is null

                if(Text_ip.getText().toString().length() != 0)
                {
                    ip = Text_ip.getText().toString();
                    init_webView();
                }

                if(Text_port.getText().toString().length() != 0)
                {
                    udp_port = Integer.parseInt(Text_port.getText().toString());
                }

                if(Text_port_vid.getText().toString().length() != 0)
                {
                    video_port = Integer.parseInt(Text_port_vid.getText().toString());
                    init_webView();
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
        final TextView textView_info = (TextView) findViewById(R.id.textView_info);
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

        //When Joystick is not moving
        get_joystick_data(0, 0);
        messageStr_tx = make_message();
        Communication(messageStr_tx);

        //When Joystick is moving
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                global_angle = angle;
                global_strength = strength;
                get_joystick_data(angle, strength);
                messageStr_tx = make_message();
                Communication(messageStr_tx);
                textView_info.setText("Angle: "+angle+" "+"Strength: "+strength);
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
                    // toggle_on_off.setEnabled(true);
                    toggle_on_off.setChecked(false);
                    toggle_on_off.setVisibility(View.VISIBLE);
                    mode_flag = 1; // Auto
                    control_flag = 0; // Force Cam control
                    autonomous_flag = 0;
                    // textView_info.setText("Mode: Automatic");
                    textView_info.setText("Automatic mode not available");
                    messageStr_tx = make_message();
                    Communication(messageStr_tx);

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
                    textView_info.setText("Mode: Manual");
                    messageStr_tx = make_message();
                    Communication(messageStr_tx);
                }
            }

        });

        switch_car_cam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // If the switch button is on ie Car
                    mode_flag = 0; //Manual
                    control_flag = 1; // Car control
                    autonomous_flag = 0;

                    textView_info.setText("Control: Car");
                    messageStr_tx = make_message();
                    Communication(messageStr_tx);

                } else {
                    // If the switch button is off ie cam
                    mode_flag = 0; //Manual
                    control_flag = 0; // Cam control
                    autonomous_flag = 0;

                    textView_info.setText("Control: Cam");
                    messageStr_tx = make_message();
                    Communication(messageStr_tx);
                }
            }

        });

        toggle_on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    mode_flag = 1; //Auto
                    control_flag = 0; // Force Cam control
                    autonomous_flag = 1;

                    textView_info.setText("Autonomous: Enabled");
                    messageStr_tx = make_message();
                    Communication(messageStr_tx);
                } else {
                    // The toggle is disabled
                    mode_flag = 1; //Auto
                    control_flag = 0; // Force Cam control
                    autonomous_flag = 0;

                    textView_info.setText("Autonomous: Disabled");
                    messageStr_tx = make_message();
                    Communication(messageStr_tx);
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

        if(mode_flag==0 && control_flag ==0)
        {
            //Manual + Cam Control
            cmd_mode = 0;
            control_mode = 0;
        }
        else if(mode_flag==0 && control_flag==1)
        {
            //Manual + Car Control
            cmd_mode = 0;
            control_mode = 1;
        }
        else if (mode_flag==1 && autonomous_flag ==0)
        {
            //Automatic stop + Cam Control
            cmd_mode = 1;
            control_mode = 0;
        }
        else if (mode_flag==1 && autonomous_flag==1)
        {
            //Automatic start + Cam Control
            cmd_mode = 1;
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
            new AlertDialog.Builder(this).setMessage("ARORA will be disconnected")
                    .setTitle("Exit?").setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
    public void get_joystick_data(int angle, int strength)
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
    }

    public String make_message()
    {
        set_mode_using_flags();
        set_udp_serial_num();

        /*
        The commands are seperated by commas
         1. Start byte <ARORA>
         2. Serial Number
         3. Mode <0: Manual, 1: Automatic>
         4. Control <0: Cam, 1: Car>
         5. PWM Set_speed <0 to 100>
         6. Car Forward_Backward Bit <1: Forward, 0: Stop, -1: Backward>
         7. Car_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         8. Cam Up_Down Bit <1: Up, 0: Stop, -1: Down>
         9. Cam_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         9. Cam_Left_Right Bit <1: Right, 0: Stop, -1: Left>
         */

        List<String> cmds = new ArrayList<String>();
        cmds.add(header);
        cmds.add(Integer.toString(udp_serial_no));
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

    public void set_udp_serial_num() {

         //Check for max int value. If max, reset. Else increment
        if (udp_serial_no == Integer.MAX_VALUE)
        {
            reset_udp_serial_num();
        }
        else
        {
            udp_serial_no += 1;
        }

    }

    public void reset_udp_serial_num() {

        udp_serial_no = 0;
    }

    public void Communication(String messageStr_tx)
    {
        udpClientThread = new deodates.arora.UdpClientThread(
                ip,
                udp_port,
                messageStr_tx,
                udpClientHandler);

        udpClientThread.start();
    }

}
