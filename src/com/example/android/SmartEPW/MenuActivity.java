package com.example.android.SmartEPW;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.example.android.SmartEPW.R;

import java.util.List;

public class MenuActivity extends FragmentActivity implements View.OnClickListener{

    private WifiManager wifiManager;

    private ResideMenu resideMenu;
    private MenuActivity mContext;
    private ResideMenuItem itemHome;
    private ResideMenuItem itemControllEPW;
    private ResideMenuItem itemDebug;
    private ResideMenuItem itemHelp;

    private Home_Fragment mHome_Fragment = new Home_Fragment();
    private ControlEPW_Fragment mControlEPW_Fragment = new ControlEPW_Fragment();
    private Debug_Fragment mDebug_Fragment = new Debug_Fragment();
    private Help_Fragment mHelp_Fragment = new Help_Fragment();


    /*declare to menu name and icon*/
    private String titles[] = { "Home", "Control", "Debug", "Help" };
    private int icon[] = { R.drawable.icon_home,
                   R.drawable.icon_profile,
                   R.drawable.icon_settings,
                   R.drawable.icon_calendar };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main); /*the main layout is based on by all of fragment  */

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        //changeFragment(new HomeFragment());
        setUpMenu();
        /*start menu display to user*/
        resideMenu.openMenu();

    }




    private void setUpMenu() {

        // attach to current activity;
        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.menu_background);
        resideMenu.attachToActivity(this);
        resideMenu.setMenuListener(menuListener);

        // create menu items;
        itemHome     = new ResideMenuItem(this, icon[0], titles[0]);
        itemControllEPW  = new ResideMenuItem(this, icon[1], titles[1]);
        itemDebug = new ResideMenuItem(this, icon[2], titles[2]);
        itemHelp = new ResideMenuItem(this, icon[3], titles[3]);

        /** when the following view is on clicked, it will be return itself view to this view,
         *  and send to this onClick method.
         */
        itemHome.setOnClickListener(this);
        itemControllEPW.setOnClickListener(this);
        itemDebug.setOnClickListener(this);
        itemHelp.setOnClickListener(this);

        // add all items to menu;
        resideMenu.addMenuItem(itemHome);
        resideMenu.addMenuItem(itemControllEPW);
        resideMenu.addMenuItem(itemDebug);
        resideMenu.addMenuItem(itemHelp);

        findViewById(R.id.title_bar_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resideMenu.openMenu();
            }
        });

        /*display layout first .*/
        changeFragment(mHome_Fragment    ,  titles[0] );
    }

    @Override
    public void onClick(View view) {
        if (view == itemHome){
            changeFragment(mHome_Fragment    ,  titles[0] );
        }else if (view == itemControllEPW){
            changeFragment(mControlEPW_Fragment  ,  titles[1] );
        }else if (view == itemDebug){
            changeFragment(mDebug_Fragment ,  titles[2] );
        }else if (view == itemHelp){
            changeFragment(mHelp_Fragment ,  titles[3] );
        }
        resideMenu.closeMenu();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return resideMenu.onInterceptTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    private ResideMenu.OnMenuListener menuListener = new ResideMenu.OnMenuListener() {
        @Override
        public void openMenu() {
            Toast.makeText(mContext, "Menu is opened!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void closeMenu() {
            Toast.makeText(mContext, "Menu is closed!", Toast.LENGTH_SHORT).show();
        }
    };


    // Dynamic instantiate a new fragment.
    private void changeFragment(Fragment targetFragment ,String tagMsg){
        resideMenu.clearIgnoredViewList();
        getSupportFragmentManager()/*if you don't below 3.0 android sdk , you can take it "Support" */
                .beginTransaction()
                .replace(R.id.main_layout, targetFragment, tagMsg)
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }


    // What good method is to access resideMenu？
    public ResideMenu getResideMenu(){
        return resideMenu;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mControlEPW_Fragment.isVisible() && !resideMenu.isOpened()) {
            mControlEPW_Fragment.myOnKeyDown(keyCode , event);
        }
        else if (mDebug_Fragment.isVisible() && !resideMenu.isOpened()){
            mDebug_Fragment.myOnKeyDown(keyCode , event);
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean isHomeMemu(){
        String packageName = this.getPackageName();

        ActivityManager mActivityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
        if (true) Log.i("CurrentActivityName", "The CurrentActivityName is :"+ rti.get(1).topActivity.toString());

        return rti.get(0).topActivity.toString().contains(packageName);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}
