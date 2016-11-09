package com.and2long.gaodemap;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements LocationSource {

    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST = 100;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener;

    private TextView tvResult;
    private ProgressDialog progressDialog;
    private MapView mMapView;
    private AMap aMap;
    private UiSettings mUiSettings;     //定义一个UiSettings对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("高德地图");

        tvResult = (TextView) findViewById(R.id.tv_result);
        mMapView = (MapView) findViewById(R.id.map);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.positioning));
        progressDialog.setCancelable(false);

        registerLocationListener();
        //实现地图生命周期管理
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
            mUiSettings = aMap.getUiSettings();//实例化UiSettings类
            //mUiSettings.setCompassEnabled(true);
            mUiSettings.setZoomControlsEnabled(false);
            aMap.setLocationSource(this);// 设置定位监听
            mUiSettings.setMyLocationButtonEnabled(true); // 显示默认的定位按钮
            aMap.setMyLocationEnabled(true);// 可触发定位并显示定位层
            //mUiSettings.setScaleControlsEnabled(true);//显示比例尺控件
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，实现地图生命周期管理
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stopLocation();
        mLocationClient.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    private void registerLocationListener() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                progressDialog.dismiss();
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        //可在其中解析amapLocation获取相应内容。
                        StringBuffer stringBuffer = new StringBuffer();
                        double latitude = aMapLocation.getLatitude();
                        stringBuffer.append("纬度：" + latitude + "\n");
                        double longitude = aMapLocation.getLongitude();
                        stringBuffer.append("经度：" + longitude + "\n");
                        //stringBuffer.append("精度：" + aMapLocation.getAccuracy() + "\n");
                        String address = aMapLocation.getProvince()
                                + aMapLocation.getCity()
                                + aMapLocation.getDistrict()
                                + aMapLocation.getStreet()
                                + aMapLocation.getStreetNum();
                        stringBuffer.append("地址：" + address + "\n");
                        //获取定位时间
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = new Date(aMapLocation.getTime());
                        String time = df.format(date);
                        stringBuffer.append(time + "");
                        tvResult.setText(stringBuffer.toString());
                        //添加标记点（移除上一个标记）。
                        addMarker(latitude, longitude);
                        //地图移动到当前位置。
                        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(aMapLocation.getLatitude(),
                                        aMapLocation.getLongitude()),
                                19));
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.location_error), Toast.LENGTH_SHORT).show();
                        //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError", "location Error, ErrCode:"
                                + aMapLocation.getErrorCode() + ", errInfo:"
                                + aMapLocation.getErrorInfo());
                    }
                }
            }
        };
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
                .setOnceLocation(true)      //获取一次定位结果
                .setNeedAddress(true);      //返回地址信息
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    private void checkPermissionsAndDoNext() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            //如果app之前请求过该权限,被用户拒绝, 这个方法就会返回true.
            //如果用户之前拒绝权限的时候勾选了对话框中"Don’t ask again"的选项,那么这个方法会返回false.
            //如果设备策略禁止应用拥有这条权限, 这个方法也返回false.
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                //提示用户需要权限
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.help)
                        .setCancelable(false)
                        .setMessage(R.string.message_need_permission)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //进入设置中的应用信息详情页，让用户手动授权
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .create()
                        .show();
            } else {
                //没有权限，请求权限。
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST);
            }
        } else {
            //具有权限，执行操作
            progressDialog.show();
            mLocationClient.startLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //已授权
                    progressDialog.show();
                    mLocationClient.startLocation();
                } else {
                    //拒绝

                }
                break;


        }
    }


    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        Log.i(TAG, "activate: 此方法被回调。");
        checkPermissionsAndDoNext();
    }

    @Override
    public void deactivate() {

    }


    private void addMarker(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        aMap.clear();
        aMap.addMarker(new MarkerOptions()
                .position(latLng));

    }
}
