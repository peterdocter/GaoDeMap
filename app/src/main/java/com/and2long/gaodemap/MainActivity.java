package com.and2long.gaodemap;

import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviStaticInfo;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;
import com.autonavi.tbt.NaviStaticInfo;
import com.autonavi.tbt.TrafficFacilityInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationSource, AMapLocationListener, AMap.OnCameraChangeListener, AMap.OnMarkerClickListener, AMapNaviListener, AMap.OnMapClickListener, View.OnClickListener {

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
    private UiSettings mUiSettings;     //定义一个UiSettings对象
    private int width; //屏幕宽度
    private int height;//屏幕高度
    //屏幕移动过程中的中心Marker
    private Marker pinMarker;
    // 中心点经纬度
    private LatLng centerLl;
    // 起点坐标
    private NaviLatLng mNaviStart;
    // 终点坐标
    private NaviLatLng mNaviEnd;
    // 高德导航界面
    private AMapNavi mAMapNavi;
    //导航起点标记
    private Marker mStartMarker;
    //导航终点标记
    private Marker mEndMarker;
    private AMap mAmap;
    private Button btnNavi;
    //地图是否可点击
    //private boolean mapClickStartReady;
    private boolean mapClickEndReady;
    //起点坐标
    private NaviLatLng startLatlng;
    //终点坐标
    private NaviLatLng endLatlng;
    //起点坐标集合
    private List<NaviLatLng> startList = new ArrayList<>();
    //终点坐标集合
    private List<NaviLatLng> endList = new ArrayList<>();
    //保存当前算好的路线
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("高德地图");
        init();
        //初始化控件
        initView();
        //初始化地图
        initMap(savedInstanceState);
        //初始化定位
        initLocation();
        //初始化导航
        initNavi();
        //开启定位
        startLocation();
    }

    private void init() {
        // 屏幕像素获得
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
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


    /**
     * 初始化控件
     */
    private void initView() {
        tvResult = (TextView) findViewById(R.id.tv_result);
        mMapView = (MapView) findViewById(R.id.map);
        btnNavi = (Button) findViewById(R.id.btn_navi);
        btnNavi.setOnClickListener(this);
        //等待提示框
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.positioning));
        progressDialog.setCancelable(false);
    }

    /**
     * 初始化地图
     *
     * @param savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        if (mAmap == null) {
            mAmap = mMapView.getMap();
        }
        mUiSettings = mAmap.getUiSettings();//实例化UiSettings类
        mUiSettings.setZoomControlsEnabled(true);       //显示缩放控件
        mUiSettings.setMyLocationButtonEnabled(true); // 显示默认的定位按钮
        mAmap.setLocationSource(this);// 设置定位监听
        mAmap.setMyLocationEnabled(true);// 可触发定位并显示定位层
        //设置地图移动监听
        mAmap.setOnCameraChangeListener(this);
        mAmap.setOnMapClickListener(this);
        //卫星模式
        mAmap.setMapType(AMap.MAP_TYPE_NORMAL);
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位请求时间间隔
        mLocationOption.setHttpTimeOut(5000);
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
                //获取一次定位结果
                .setOnceLocation(true)
                //返回地址信息
                .setNeedAddress(true);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    /**
     * 初始化导航
     */
    private void initNavi() {
        //获取AMapNavi实例
        mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        //添加监听回调，用于处理算路成功
        mAMapNavi.addAMapNaviListener(this);
        // 初始化Marker添加到地图
        /*mStartMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.start))));
        mEndMarker = mAmap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.end))));
*/
    }

    /**
     * 添加标记点
     *
     * @param latitude
     * @param longitude
     */
    private void addMarker(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        mAmap.clear();
        mAmap.addMarker(new MarkerOptions()
                .position(latLng));

    }

    /**
     * 定位按钮被点击时回调
     *
     * @param onLocationChangedListener
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        Log.i(TAG, "activate: 定位按钮被点击");
        startLocation();
    }

    /**
     * 启动定位.
     */
    private void startLocation() {
        progressDialog.show();
        if (mLocationClient == null) {
            return;
        }
        mLocationClient.startLocation();
    }

    @Override
    public void deactivate() {

    }

    /**
     * 定位信息改变
     *
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        Log.i(TAG, "onLocationChanged: 位置信息改变");
        progressDialog.dismiss();
        mAmap.clear();
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
                Log.i(TAG, "onLocationChanged: 定位成功");
                //地图移动到当前位置。
                mAmap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(aMapLocation.getLatitude(),
                                aMapLocation.getLongitude()),
                        18));
                //导航起点
                startLatlng = new NaviLatLng(latitude, longitude);
                startList.clear();
                startList.add(startLatlng);
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.location_error), Toast.LENGTH_SHORT).show();
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    /**
     * 地图拖拽事件
     *
     * @param cameraPosition
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.i(TAG, "onCameraChange: 地图被拖拽");
        if (pinMarker != null) {
            pinMarker.remove();
        }

    }

    /**
     * 地图拖拽完毕
     *
     * @param cameraPosition
     */
    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        Log.i(TAG, "onCameraChangeFinish: 地图拖拽完毕");
        /*//添加标签
        pinMarker = aMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_pin)).draggable(false).draggable(false));
        pinMarker.setPositionByPixels(width / 2, height / 2);
        //中心点坐标
        centerLl = cameraPosition.target;
        //导航终点
        mNaviEnd = new NaviLatLng(centerLl.latitude, centerLl.longitude);
        calculateDriveRoute();*/
    }

    /**
     * 设置marker点击事件
     *
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i(TAG, "onMarkerClick: Marker被点击");
        return false;
    }

    /**
     * 计算驾车路线
     */
    private void calculateDriveRoute() {
        //clearRoute();
        mapClickEndReady = false;
        int strategyFlag = 0;
        try {
            strategyFlag = mAMapNavi.strategyConvert(true, false, false, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (strategyFlag >= 0) {
            mAMapNavi.calculateDriveRoute(startList, endList, null, strategyFlag);
            Toast.makeText(getApplicationContext(), "策略:" + strategyFlag, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInitNaviFailure() {

    }

    /**
     * 导航初始化成功
     */
    @Override
    public void onInitNaviSuccess() {
        Log.i(TAG, "onInitNaviSuccess: 导航初始化成功");
    }

    /**
     * 线路规划成功
     */
    @Override
    public void onCalculateRouteSuccess() {
        Log.i(TAG, "onCalculateRouteSuccess: 单一策略,线路规划成功");
        routeOverlays.clear();
        AMapNaviPath path = mAMapNavi.getNaviPath();
        /**
         * 单路径不需要进行路径选择，直接传入－1即可
         */
        drawRoutes(-1, path);
    }

    /**
     * 多线路规划成功
     *
     * @param ints
     */
    @Override
    public void onCalculateMultipleRoutesSuccess(int[] ints) {
        Log.i(TAG, "onCalculateMultipleRoutesSuccess: 多策略,线路规划成功");
    }

    /**
     * 算路失败
     *
     * @param i
     */
    @Override
    public void onCalculateRouteFailure(int i) {
        Log.i(TAG, "onCalculateRouteFailure: 算路失败");
    }

    private void drawRoutes(int routeId, AMapNaviPath path) {
        mAmap.moveCamera(CameraUpdateFactory.changeTilt(0));
        RouteOverLay routeOverLay = new RouteOverLay(mAmap, path, this);
        routeOverLay.setTrafficLine(true);
        routeOverLay.addToMap();
        routeOverlays.put(routeId, routeOverLay);
    }

    @Override
    public void onStartNavi(int i) {
        Log.i(TAG, "onStartNavi: 开始导航");
    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {
        Log.i(TAG, "onLocationChange: ");
    }

    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onArriveDestination(NaviStaticInfo naviStaticInfo) {

    }

    @Override
    public void onArriveDestination(AMapNaviStaticInfo aMapNaviStaticInfo) {

    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    /**
     * 地图点击事件
     *
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {
        //控制选终点
        if (mapClickEndReady) {
            endLatlng = new NaviLatLng(latLng.latitude, latLng.longitude);
            mAmap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource
                            (getResources(), R.mipmap.end)))
                    .position(latLng));
            //mEndMarker.setPosition(latLng);
            endList.clear();
            endList.add(endLatlng);
            mapClickEndReady = false;
            //开始计算驾车路线
            calculateDriveRoute();
        }
    }

    /**
     * 控件的点击事件
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_navi:
                //设置地图可点击
                mapClickEndReady = true;
                Toast.makeText(this, "选择终点", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
