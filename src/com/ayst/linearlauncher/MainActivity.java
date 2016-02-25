package com.ayst.linearlauncher;

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ayst.linearlauncher.utils.BackDoor;
import com.ayst.linearlauncher.utils.HidePackageList;
import com.ayst.linearlauncher.widget.CircleImageView;
import com.ayst.linearlauncher.widget.twoway.TwoWayAdapterView;
import com.ayst.linearlauncher.widget.twoway.TwoWayGridView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";
    private TwoWayGridView mGridWay = null;
    private MainAdapter mAdapter = null;
    private PackageManager mPkgManager = null;

    private BackDoor mBackDoorHide = null;
    private BackDoor mBackDoorReset = null;

    private ArrayList<String> mHidePackageList = null;

    private LinearLayout mBottomView = null;
    private CircleImageView mBottomIcon = null;
    private TextView mBottomText = null;
    private Button mBottomHideBtn = null;
    private Button mBottomUninstallBtn = null;
//    private TwoWayGridView mBottomGridView = null;
//    private MainAdapter mBottomAdapter = null;

    private PackageChangedReceiver mPkgChangedReceiver = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPkgManager = this.getPackageManager();
        initData();
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mPkgChangedReceiver = new PackageChangedReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        registerReceiver(mPkgChangedReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mPkgChangedReceiver != null) {
            unregisterReceiver(mPkgChangedReceiver);
        }
    }

    private void initData() {
        mHidePackageList = HidePackageList.get(this);

        mAdapter = new MainAdapter(this, R.layout.main_item);
        mAdapter.addAll(getAllApps(this, true));

//        mBottomAdapter = new MainAdapter(this, R.layout.bottom_item);
//        mBottomAdapter.addAll(getAllApps(this, false));

        mBackDoorHide = new BackDoor(BackDoor.DOORKEY_HIDE);
        mBackDoorReset = new BackDoor(BackDoor.DOORKEY_RESET);
    }

    private void initView() {
        mGridWay = (TwoWayGridView) findViewById(R.id.twoway_gridview);
        mGridWay.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(TwoWayAdapterView<?> parent, View view,
                                    int position, long id) {
                Log.i(TAG, "mGridWay onItemClick position=" + position);
                if (position >= 0 && position < mAdapter.getCount()) {
                    ResolveInfo item = mAdapter.getItem(position);
                    if (item != null) {
                        //该应用的包名
                        String pkg = item.activityInfo.packageName;
                        //应用的主activity类
                        String cls = item.activityInfo.name;
                        ComponentName componet = new ComponentName(pkg, cls);
                        try {
                            Intent intent = new Intent();
                            intent.setComponent(componet);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(TAG, "Item is null");
                    }
                }
            }
        });
        mAdapter.setFixMargin(mGridWay.getMarginLeftOrRight());
        mGridWay.setAdapter(mAdapter);

        mBottomView = (LinearLayout) findViewById(R.id.ll_bottom);
        mBottomIcon = (CircleImageView) findViewById(R.id.bottom_icon);
        mBottomText = (TextView) findViewById(R.id.bottom_text);
        mBottomHideBtn = (Button) findViewById(R.id.btn_hide);
        mBottomUninstallBtn = (Button) findViewById(R.id.btn_del);
//        mBottomGridView = (TwoWayGridView) findViewById(R.id.bottom_gridview);
//        mBottomGridView.setOnItemClickListener(new TwoWayAdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(TwoWayAdapterView<?> parent, View view,
//                                    int position, long id) {
//                Log.i(TAG, "mBottomGridView onItemClick position=" + position);
//                if (position >= 0 && position < mBottomAdapter.getCount()) {
//                    ResolveInfo item = mBottomAdapter.getItem(position);
//                    if (item != null) {
//                    } else {
//                        Log.i(TAG, "Item is null");
//                    }
//                }
//            }
//        });
//        mBottomGridView.setAdapter(mBottomAdapter);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mBackDoorHide.isAsRule(event)) {
            ResolveInfo selectedItem = (ResolveInfo) mGridWay.getSelectedItem();
            Log.i(TAG, "dispatchKeyEvent, BACKDOOR_HIDE selected package=" + selectedItem.activityInfo.packageName);
            mHidePackageList.add(selectedItem.activityInfo.packageName);
            HidePackageList.save(this, mHidePackageList);
            update();
        } else if (mBackDoorReset.isAsRule(event)) {
            Log.i(TAG, "dispatchKeyEvent, BACKDOOR_RESET");
            mHidePackageList.clear();
            HidePackageList.save(this, mHidePackageList);
            update();
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (mBottomView.getVisibility() == View.GONE) {
                ResolveInfo selectedItem = (ResolveInfo) mGridWay.getSelectedItem();
                mBottomIcon.setImageDrawable(selectedItem.loadIcon(mPkgManager));
                mBottomText.setText(selectedItem.loadLabel(mPkgManager));
                mBottomHideBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mHidePackageList.add(selectedItem.activityInfo.packageName);
                        HidePackageList.save(MainActivity.this, mHidePackageList);
                        mBottomView.setVisibility(View.GONE);
                        update();
                    }
                });
                mBottomUninstallBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        uninstall(selectedItem.activityInfo.packageName);
                        mBottomView.setVisibility(View.GONE);
                    }
                });
                mBottomView.setVisibility(View.VISIBLE);
                mBottomHideBtn.requestFocus();
            } else {
                mBottomView.setVisibility(View.GONE);
            }
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (mBottomView.getVisibility() == View.VISIBLE) {
                mBottomView.setVisibility(View.GONE);
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    public List<ResolveInfo> getAllApps(Context context, boolean isGetShow) {
        List<ResolveInfo> apps = new ArrayList<ResolveInfo>();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> appsAll = context.getPackageManager().queryIntentActivities(intent, 0);

        boolean isHide = false;
        for (ResolveInfo item : appsAll) {
            isHide = false;
            for (String pkg : mHidePackageList) {
                if (item.activityInfo.packageName.equals(pkg)) {
                    Log.i(TAG, "getAllApps, isHide pkg=" + pkg);
                    if (!isGetShow) {
                        apps.add(item);
                    }
                    isHide = true;
                    break;
                }
            }
            if (!isHide && isGetShow) {
                apps.add(item);
            }
        }

        return apps;
    }

    private void update() {
        mAdapter.clear();
        mAdapter.addAll(getAllApps(this, true));
        mAdapter.notifyDataSetChanged();
    }

    private void uninstall(String pkgName) {
        Uri uri = Uri.parse("package:" + pkgName);//获取删除包名的URI
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_DELETE);//设置我们要执行的卸载动作
        intent.setData(uri);//设置获取到的URI
        startActivity(intent);
    }

    class PackageChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //接收安装广播
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
                String packageName = intent.getDataString();
                Log.i(TAG, "install:" + packageName);
                update();
            }
            //接收卸载广播
            if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
                String packageName = intent.getDataString();
                Log.i(TAG, "uninstall:" + packageName);
                update();
            }
        }
    }
}
