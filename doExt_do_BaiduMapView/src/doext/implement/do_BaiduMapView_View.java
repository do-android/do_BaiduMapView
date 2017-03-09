package doext.implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.ArcOptions;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMapStatusChangeListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.InfoWindow.OnInfoWindowClickListener;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiBoundSearchOption;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoImageLoadHelper;
import core.helper.DoJsonHelper;
import core.helper.DoResourcesHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIModuleTypeID;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoProperty;
import core.object.DoUIModule;
import doext.define.do_BaiduMapView_IMethod;
import doext.define.do_BaiduMapView_MAbstract;
import doext.overlay.BikingRouteOverlay;
import doext.overlay.DrivingRouteOverlay;
import doext.overlay.TransitRouteOverlay;
import doext.overlay.WalkingRouteOverlay;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_Label_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_BaiduMapView_View extends FrameLayout implements DoIUIModuleView, do_BaiduMapView_IMethod, DoIModuleTypeID, OnGetRoutePlanResultListener, MKOfflineMapListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_BaiduMapView_MAbstract model;
	private ViewGroup mapView;
	private BaiduMap baiduMap;
	private Map<String, Marker> markers;
	private Map<String, Overlay> overlays;
	private Context mContext;
	private String popWindowId;
	private LatLng latLng;
	// 搜索相关
	RoutePlanSearch mSearch = null; // 搜索模块，也可去掉地图模块独立使用

	private int mapScene;
	private MKOfflineMap mKOfflineMap;

	public do_BaiduMapView_View(Context context) {
		super(context);
		SDKInitializer.initialize(context.getApplicationContext());
		this.mContext = context;

	}

	private void doBaiduMapView_RegionChange(LatLng latLng) {
		try {
			DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
			JSONObject _obj = new JSONObject();
			_obj.put("latitude", latLng.latitude);
			_obj.put("longitude", latLng.longitude);
			_invokeResult.setResultNode(_obj);
			model.getEventCenter().fireEvent("regionChange", _invokeResult);
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View regionChange event\n\t", e);
		}
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_BaiduMapView_MAbstract) _doUIModule;
		DoProperty _property = this.model.getProperty("mapScene");
		if (_property != null) {
			mapScene = DoTextHelper.strToInt(_property.getValue(), 0);
		}
		if (mapScene == 0) {
			mapView = new TextureMapView(mContext);
			((TextureMapView) mapView).showZoomControls(false);
			baiduMap = ((TextureMapView) mapView).getMap();
		} else {
			mapView = new MapView(mContext);
			((MapView) mapView).showZoomControls(false);
			baiduMap = ((MapView) mapView).getMap();
		}

		FrameLayout.LayoutParams fParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		this.addView(mapView, fParams);

		baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
		baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(10).build()));
		markers = new HashMap<String, Marker>();
		overlays = new HashMap<String, Overlay>();
		baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {

			@Override
			public boolean onMarkerClick(Marker arg0) {
				String id;
				// 显示弹窗
				Button _pop = new Button(mContext);
				try {
					id = arg0.getExtraInfo().getString("id");
				} catch (Exception e) {
					return false;
				}
				String info = arg0.getExtraInfo().getString("info");
				final String data = arg0.getExtraInfo().getString("data");
				if (info != null)
					_pop.setText(info);
				_pop.setTextSize(13f);
				int _popupId = DoResourcesHelper.getIdentifier("popup", "drawable", do_BaiduMapView_View.this);
				_pop.setBackgroundResource(_popupId);
				_pop.setGravity(Gravity.CENTER);

				InfoWindow mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(_pop), arg0.getPosition(), -47, new OnInfoWindowClickListener() {
					public void onInfoWindowClick() {
						doBaiduMapView_TouchMarker(data);
					}
				});
				popWindowId = id;
				baiduMap.showInfoWindow(mInfoWindow);

				// 标记点击事件回调
				doBaiduMapView_TouchMarker(data);

				return true;
			}
		});

		baiduMap.setOnMapStatusChangeListener(new OnMapStatusChangeListener() {
			@Override
			public void onMapStatusChange(MapStatus status) {
				latLng = status.target;
			}

			@Override
			public void onMapStatusChangeFinish(MapStatus status) {
				model.setPropertyValue("zoomLevel", status.zoom + "");
				LatLng _latLng = status.target;
				doBaiduMapView_RegionChange(_latLng);
			}

			@Override
			public void onMapStatusChangeStart(MapStatus status) {
			}
		});

		baiduMap.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public boolean onMapPoiClick(MapPoi poi) {
				baiduMap.hideInfoWindow();
				doBaiduMapView_TouchMap(poi.getPosition());
				return false;
			}

			@Override
			public void onMapClick(LatLng latLng) {
				baiduMap.hideInfoWindow();
				doBaiduMapView_TouchMap(latLng);
			}
		});
		// 初始化搜索模块，注册事件监听
		mSearch = RoutePlanSearch.newInstance();
		mSearch.setOnGetRoutePlanResultListener(this);

		mKOfflineMap = new MKOfflineMap();
		mKOfflineMap.init(this);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("zoomLevel")) {
			float _zoomLevel = DoTextHelper.strToFloat(_changedValues.get("zoomLevel"), 10.0f);
			baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(_zoomLevel).build()));
			doBaiduMapView_RegionChange(latLng);
		}

		if (_changedValues.containsKey("mapType")) {
			String _mapType = _changedValues.get("mapType");
			if ("satellite".equals(_mapType)) {
				baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
			} else {
				baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
			}
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("setCenter".equals(_methodName)) {
			this.setCenter(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("addMarkers".equals(_methodName)) {
			this.addMarkers(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("removeMarker".equals(_methodName)) {
			this.removeMarker(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("removeAll".equals(_methodName)) {
			this.removeAll(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("getDistance".equals(_methodName)) {
			this.getDistance(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("addOverlay".equals(_methodName)) {
			this.addOverlay(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("removeOverlay".equals(_methodName)) {
			this.removeOverlay(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}

		if ("pauseDownload".equals(_methodName)) {
			this.pauseDownload(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("removeDownload".equals(_methodName)) {
			this.removeDownload(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	private void addOverlay(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _id = DoJsonHelper.getString(_dictParas, "id", "");
		if (overlays.containsKey(_id)) {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView addOverlay", new Exception("id为" + _id + "已经存在！"));
			return;
		}
		int _type = DoJsonHelper.getInt(_dictParas, "type", 0);
		Object _data = DoJsonHelper.get(_dictParas, "data");

		int _fillColor = DoUIModuleHelper.getColorFromString(DoJsonHelper.getString(_dictParas, "fillColor", ""), Color.TRANSPARENT);
		int _strokeColor = DoUIModuleHelper.getColorFromString(DoJsonHelper.getString(_dictParas, "strokeColor", ""), Color.BLACK);
		int _width = DoJsonHelper.getInt(_dictParas, "width", 5);
		boolean _isDash = DoJsonHelper.getBoolean(_dictParas, "isDash", false);

		if (_data == null) {
			_invokeResult.setError("data 参数不能为空！");
			throw new Exception("data 参数不能为空！");
		}
		switch (_type) {
		case 0:// 创建圆形覆盖物选项类
			if (!(_data instanceof JSONObject)) {
				_invokeResult.setError("data 参数不合法");
				throw new Exception("data 参数不合法");
			}
			JSONObject _circleParam = (JSONObject) _data;

			int _radius = DoJsonHelper.getInt(_circleParam, "radius", 0);
			CircleOptions _circleOptions = new CircleOptions();
			_circleOptions.center(getLatLng(_circleParam, _invokeResult)).radius(_radius);
			_circleOptions.fillColor(_fillColor).stroke(new Stroke(_width, _strokeColor));
			overlays.put(_id, baiduMap.addOverlay(_circleOptions));
			break;
		case 1: // 创建折线覆盖物选项类
			if (!(_data instanceof JSONArray)) {
				_invokeResult.setError("data 参数不合法");
				throw new Exception("data 参数不合法");
			}
			JSONArray _polylineParam = (JSONArray) _data;
			List<LatLng> _points = new ArrayList<LatLng>();
			for (int i = 0; i < _polylineParam.length(); i++) {
				_points.add(getLatLng(_polylineParam.getJSONObject(i), _invokeResult));
			}

			if (_points.size() > 0) {
				PolylineOptions _polylineOptions = new PolylineOptions();
				_polylineOptions.points(_points);
				_polylineOptions.color(_strokeColor);
				_polylineOptions.width(_width);
				_polylineOptions.dottedLine(_isDash);
				overlays.put(_id, baiduMap.addOverlay(_polylineOptions));
			}

			break;
		case 2:// 创建多边形覆盖物选项类
			if (!(_data instanceof JSONArray)) {
				_invokeResult.setError("data 参数不合法");
				throw new Exception("data 参数不合法");
			}
			JSONArray _polygonParam = (JSONArray) _data;
			_points = new ArrayList<LatLng>();
			for (int i = 0; i < _polygonParam.length(); i++) {
				_points.add(getLatLng(_polygonParam.getJSONObject(i), _invokeResult));
			}

			if (_points.size() > 0) {
				PolygonOptions _polygonOptions = new PolygonOptions();
				_polygonOptions.points(_points);
				_polygonOptions.fillColor(_fillColor).stroke(new Stroke(_width, _strokeColor));
				overlays.put(_id, baiduMap.addOverlay(_polygonOptions));
			}

			break;
		case 3: // 创建弧线覆盖物选项类
			if (!(_data instanceof JSONArray)) {
				_invokeResult.setError("data 参数不合法");
				throw new Exception("data 参数不合法");
			}
			JSONArray _arcParam = (JSONArray) _data;
			if (_arcParam.length() < 3) {
				_invokeResult.setError("data 参数不合法，必须是3个点的坐标");
				throw new Exception("data 参数不合法，必须是3个点的坐标");
			}
			LatLng _start = getLatLng(_arcParam.getJSONObject(0), _invokeResult);
			LatLng _middle = getLatLng(_arcParam.getJSONObject(1), _invokeResult);
			LatLng _end = getLatLng(_arcParam.getJSONObject(2), _invokeResult);
			ArcOptions _arcOptions = new ArcOptions();
			_arcOptions.points(_start, _middle, _end);
			_arcOptions.color(_strokeColor);
			_arcOptions.width(_width);
			overlays.put(_id, baiduMap.addOverlay(_arcOptions));
			break;
		default:
			throw new Exception("type 参数错误！");
		}
	}

	private LatLng getLatLng(JSONObject _obj, DoInvokeResult _invokeResult) throws Exception {
		double _latitude = DoJsonHelper.getDouble(_obj, "latitude", -1);
		double _longitude = DoJsonHelper.getDouble(_obj, "longitude", -1);
		if (_latitude > 0 && _longitude > 0) {
			return new LatLng(_latitude, _longitude);
		}
		_invokeResult.setError("经纬度不合法");
		throw new Exception("经纬度不合法");
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @throws Exception
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, final JSONObject _dictParas, final DoIScriptEngine _scriptEngine, final String _callbackFuncName) throws Exception {
		if ("poiSearch".equals(_methodName)) {
			Activity _activity = (Activity) mContext;
			// 0:城市POI检索;1:在矩形范围内POI检索;2:根据中心点、半径POI检索;
			_activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						poiSearch(_dictParas, _scriptEngine, _callbackFuncName);
					} catch (Exception e) {
						DoServiceContainer.getLogEngine().writeError("do_BaiduMapView poiSearch \r\n", e);
					}
				}
			});
			return true;
		}
		if ("routePlanSearch".equals(_methodName)) {
			this.routePlanSearch(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		if ("getHotCityList".equals(_methodName)) {
			this.getHotCityList(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		if ("startDownload".equals(_methodName)) {
			this.startDownload(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
		markers.clear();
		overlays.clear();
		baiduMap.clear();
		mKOfflineMap.destroy();
		if (mapView != null) {
			if (mapScene == 0) {
				((TextureMapView) mapView).onDestroy();
			} else {
				((MapView) mapView).onDestroy();
			}
		}

		if (null != mPoiSearch) {
			mPoiSearch.destroy();
			mPoiSearch = null;
		}
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 设置地图中心点；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void setCenter(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		Double latitude = DoJsonHelper.getDouble(_dictParas, "latitude", -1);
		Double longitude = DoJsonHelper.getDouble(_dictParas, "longitude", -1);

		if (latitude > 0 && longitude > 0) {
			// 设定中心点坐标
			LatLng cenpt = new LatLng(latitude, longitude);
			doBaiduMapView_RegionChange(cenpt);
			// 定义地图状态
			MapStatus mMapStatus = new MapStatus.Builder().target(cenpt).build();
			// 定义MapStatusUpdate对象，以便描述地图状态将要发生的变化

			MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
			// 改变地图状态
			baiduMap.setMapStatus(mMapStatusUpdate);
			_invokeResult.setResultBoolean(true);
		} else {
			_invokeResult.setResultBoolean(false);
			_invokeResult.setError("中心点经纬度不合法");
			throw new Exception("中心点经纬度不合法");
		}

	}

	/**
	 * 添加一组标记；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void addMarkers(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {

		try {
			JSONArray dataArray = DoJsonHelper.getJSONArray(_dictParas, "data");
			for (int i = 0; i < dataArray.length(); i++) {
				JSONObject childData = dataArray.getJSONObject(i);
				String id = DoJsonHelper.getString(childData, "id", "");
				if (markers.containsKey(id)) {
					DoServiceContainer.getLogEngine().writeError("do_BaiduMapView addMarkers \n\t", new Exception("id为" + id + "已经存在！"));
					continue;
				}
				Double latitude = DoJsonHelper.getDouble(childData, "latitude", 39.915174);
				Double longitude = DoJsonHelper.getDouble(childData, "longitude", 116.403901);
				String url = DoJsonHelper.getString(childData, "url", "");
				String info = DoJsonHelper.getString(childData, "info", "");
				LatLng latLng = new LatLng(latitude, longitude);
				Bundle bundle = new Bundle();
				bundle.putString("id", id);
				bundle.putString("info", info);
				bundle.putString("data", childData.toString());
				// 构建Marker图标
				BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(getLocalBitmap(url));
				// 构建MarkerOption，用于在地图上添加Marker
				OverlayOptions option = new MarkerOptions().position(latLng).icon(bitmap).title(id).zIndex(6).extraInfo(bundle).perspective(true);
				// 在地图上添加Marker，并显示
				Marker marker = (Marker) baiduMap.addOverlay(option);
				markers.put(id, marker);
			}
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("添加一组标记异常", e);
			_invokeResult.setResultBoolean(false);
		}
		_invokeResult.setResultBoolean(true);
	}

	/**
	 * 移除一组指定标记；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void removeMarker(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		JSONArray _ids = DoJsonHelper.getJSONArray(_dictParas, "ids");
		for (int i = 0; i < _ids.length(); i++) {
			String _id = _ids.getString(i);
			if (markers.containsKey(_id)) {
				markers.get(_id).remove();
				markers.remove(_id);
				if (popWindowId != null && _id.equals(popWindowId)) {
					baiduMap.hideInfoWindow();
				}
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView removeMarker \r\n", new Exception("标记id:" + _id + "不存在"));
			}
		}
	}

	@Override
	public void removeOverlay(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		JSONArray _ids = DoJsonHelper.getJSONArray(_dictParas, "ids");
		for (int i = 0; i < _ids.length(); i++) {
			String _id = _ids.getString(i);
			if (overlays.containsKey(_id)) {
				overlays.get(_id).remove();
				overlays.remove(_id);
				if (popWindowId != null && _id.equals(popWindowId)) {
					baiduMap.hideInfoWindow();
				}
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView removeOverlay \r\n", new Exception("标记id:" + _id + "不存在"));
			}
		}
	}

	/**
	 * 移除所有标记；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void removeAll(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		baiduMap.hideInfoWindow();
		markers.clear();
		overlays.clear();
		baiduMap.clear();

	}

	@Override
	public void getDistance(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _startPoint = DoJsonHelper.getString(_dictParas, "startPoint", null);
		String _endPoint = DoJsonHelper.getString(_dictParas, "endPoint", null);
		if (_startPoint == null || _endPoint == null) {
			throw new Exception("startPoint 或  endPoint 参数值不能为空！");
		}
		String[] _latLng1 = _startPoint.split(",");
		String[] _latLng2 = _endPoint.split(",");
		if (_latLng1 == null || _latLng2 == null || _latLng1.length != 2 || _latLng2.length != 2) {
			throw new Exception("startPoint 或  endPoint 参数值非法！");
		}
		double _p1_lat = DoTextHelper.strToDouble(_latLng1[0], 0);
		double _p1_lng = DoTextHelper.strToDouble(_latLng1[1], 0);
		double _p2_lat = DoTextHelper.strToDouble(_latLng2[0], 0);
		double _p2_lng = DoTextHelper.strToDouble(_latLng2[1], 0);

		LatLng _p1 = new LatLng(_p1_lat, _p1_lng);
		LatLng _p2 = new LatLng(_p2_lat, _p2_lng);
		double _distance = DistanceUtil.getDistance(_p1, _p2);

		_invokeResult.setResultFloat(_distance);
	}

	private void doBaiduMapView_TouchMap(LatLng latLng) {
		try {
			DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
			JSONObject _obj = new JSONObject();
			_obj.put("latitude", latLng.latitude);
			_obj.put("longitude", latLng.longitude);
			_invokeResult.setResultNode(_obj);
			model.getEventCenter().fireEvent("touchMap", _invokeResult);
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View touchMap event\n\t", e);
		}
	}

	private void doBaiduMapView_TouchMarker(String data) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		try {
			_invokeResult.setResultNode(new JSONObject(data));
		} catch (Exception e) {
			_invokeResult.setException(e);
		}
		this.model.getEventCenter().fireEvent("touchMarker", _invokeResult);
	}

	private Bitmap getLocalBitmap(String local) throws Exception {
		Bitmap bitmap = null;
		if (null == DoIOHelper.getHttpUrlPath(local) && local != null && !"".equals(local)) {
			String path = DoIOHelper.getLocalFileFullPath(this.model.getCurrentPage().getCurrentApp(), local);
			bitmap = DoImageLoadHelper.getInstance().loadLocal(path, -1, -1);
		} else {
			throw new Exception("标记缩略图,只支持本地图片");
		}
		return bitmap;
	}

	@Override
	public String getTypeID() {
		return model.getTypeID();
	}

	private PoiSearch mPoiSearch;

	@Override
	public void poiSearch(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		int _type = DoJsonHelper.getInt(_dictParas, "type", 0);
		int _pageIndex = DoJsonHelper.getInt(_dictParas, "pageIndex", 0);
		int _pageSize = DoJsonHelper.getInt(_dictParas, "pageSize", 10);
		String _keyword = DoJsonHelper.getString(_dictParas, "keyword", "");
		if (TextUtils.isEmpty(_keyword)) {
			throw new Exception("keyword 参数不能为空！");
		}
		JSONObject _param = DoJsonHelper.getJSONObject(_dictParas, "param");
		if (_param == null) {
			throw new Exception("param 参数不能为空！");
		}
		// 0:城市POI检索;1:在矩形范围内POI检索;2:根据中心点、半径POI检索;
		mPoiSearch = PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(new MyOnGetPoiSearchResultListener(_scriptEngine, _callbackFuncName));

		switch (_type) {
		case 0:
			PoiCitySearchOption _ctiySearchOption = new PoiCitySearchOption();
			_ctiySearchOption.keyword(_keyword);
			if (_param.has("city")) {
				_ctiySearchOption.city(_param.getString("city"));
			}
			_ctiySearchOption.pageCapacity(_pageSize);
			_ctiySearchOption.pageNum(_pageIndex);
			mPoiSearch.searchInCity(_ctiySearchOption);
			break;
		case 1:
			PoiBoundSearchOption _boundSearchOption = new PoiBoundSearchOption();
			_boundSearchOption.keyword(_keyword);
			if (_param.has("leftBottom") && _param.has("rightTop")) {

				String _northeast = _param.getString("rightTop");
				String _southwest = _param.getString("leftBottom");

				String[] _latLng1 = _northeast.split(",");
				String[] _latLng2 = _southwest.split(",");
				if (_latLng1 == null || _latLng2 == null || _latLng1.length != 2 || _latLng2.length != 2) {
					throw new Exception("rightTop 或  leftBottom 参数值非法！");
				}
				double _p1_lat = DoTextHelper.strToDouble(_latLng1[0], 0);
				double _p1_lng = DoTextHelper.strToDouble(_latLng1[1], 0);
				double _p2_lat = DoTextHelper.strToDouble(_latLng2[0], 0);
				double _p2_lng = DoTextHelper.strToDouble(_latLng2[1], 0);

				LatLngBounds.Builder _builder = new LatLngBounds.Builder();
				_builder.include(new LatLng(_p1_lat, _p1_lng)).include(new LatLng(_p2_lat, _p2_lng));
				_boundSearchOption.bound(_builder.build());
			}
			_boundSearchOption.pageCapacity(_pageSize);
			_boundSearchOption.pageNum(_pageIndex);
			mPoiSearch.searchInBound(_boundSearchOption);
			break;
		case 2:

			PoiNearbySearchOption _nearbySearchOption = new PoiNearbySearchOption();
			_nearbySearchOption.keyword(_keyword);

			if (_param.has("location")) {
				String _location = _param.getString("location");

				String[] _latLng1 = _location.split(",");
				if (_latLng1 == null || _latLng1.length != 2) {
					throw new Exception("location 参数值非法！");
				}
				double _p1_lat = DoTextHelper.strToDouble(_latLng1[0], 0);
				double _p1_lng = DoTextHelper.strToDouble(_latLng1[1], 0);
				_nearbySearchOption.location(new LatLng(_p1_lat, _p1_lng));
			}

			if (_param.has("radius")) {
				_nearbySearchOption.radius(DoJsonHelper.getInt(_param, "radius", 0));
			}
			_nearbySearchOption.pageCapacity(_pageSize);
			_nearbySearchOption.pageNum(_pageIndex);
			mPoiSearch.searchNearby(_nearbySearchOption);

			break;
		default:
			throw new Exception("type 参数错误！");
		}
	}

	private class MyOnGetPoiSearchResultListener implements OnGetPoiSearchResultListener {

		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;

		public MyOnGetPoiSearchResultListener(DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
		}

		public void onGetPoiResult(PoiResult result) {
			// 获取POI检索结果
//			name:"POI名称",pt:"POI坐标",address:"POI地址",city:"POI所在城市",phone:"POI电话号码"
			List<PoiInfo> _poiList = result.getAllPoi();
			JSONArray _array = new JSONArray();
			DoInvokeResult _result = new DoInvokeResult(model.getUniqueKey());
			try {
				for (PoiInfo _info : _poiList) {
					JSONObject _obj = new JSONObject();
					_obj.put("name", _info.name);
					_obj.put("pt", _info.location.latitude + "," + _info.location.longitude);
					_obj.put("address", _info.address);
					_obj.put("city", _info.city);
					_obj.put("phone", _info.phoneNum);
					_array.put(_obj);
				}
				_result.setResultArray(_array);
			} catch (Exception e) {
				_result.setException(e);
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView poiSearch \n\t", e);
			} finally {
				this.scriptEngine.callback(callbackFuncName, _result);
			}
		}

		public void onGetPoiDetailResult(PoiDetailResult result) {
			// 获取Place详情页检索结果
		}
	}

	@Override
	public void routePlanSearch(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _type = DoJsonHelper.getString(_dictParas, "type", "");// 路线检索类型,包括Bus(公交);Ride(骑行);Walk(步行);Drive(驾车)
		String _startCityName = DoJsonHelper.getString(_dictParas, "startCityName", "");// 开始地点所在城市
		String _endCityName = DoJsonHelper.getString(_dictParas, "endCityName", "");// 结束地点所在城市
		String _startCitySite = DoJsonHelper.getString(_dictParas, "startCitySite", "");// 开始地点
		String _endCitySite = DoJsonHelper.getString(_dictParas, "endCitySite", "");// 结束地点
		
		if (!TextUtils.isEmpty(_type) && !TextUtils.isEmpty(_startCityName) && !TextUtils.isEmpty(_endCityName) && !TextUtils.isEmpty(_startCitySite) && !TextUtils.isEmpty(_endCitySite)) {
			baiduMap.clear();
			PlanNode stNode = PlanNode.withCityNameAndPlaceName(_startCityName, _startCitySite);
			PlanNode enNode = PlanNode.withCityNameAndPlaceName(_endCityName, _endCitySite);
			if (_type.equals("Bus")) {
				mSearch.transitSearch((new TransitRoutePlanOption()).from(stNode).city(_startCityName).to(enNode));
			} else if (_type.equals("Ride")) {
				mSearch.bikingSearch((new BikingRoutePlanOption()).from(stNode).to(enNode));
			} else if (_type.equals("Drive")) {
				mSearch.drivingSearch((new DrivingRoutePlanOption()).from(stNode).to(enNode));
			} else if (_type.equals("Walk")) {
				mSearch.walkingSearch((new WalkingRoutePlanOption()).from(stNode).to(enNode));
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("routePlanSearch type类型不存在"));
			}
		} else {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("请检查routePlanSearch参数是否为空"));
		}
	}

	// 骑行路线
	@Override
	public void onGetBikingRouteResult(BikingRouteResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("onGetBikingRouteResult:抱歉，未找到结果"));
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			// 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
			// result.getSuggestAddrInfo()
			return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			if (result.getRouteLines().size() >= 1) {

				BikingRouteOverlay overlay = new BikingRouteOverlay(baiduMap);
				overlay.setData(result.getRouteLines().get(0));
				overlay.addToMap();
				overlay.zoomToSpan();

			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("BikingRouteResult 结果数<0"));
				return;
			}
		}
	}

	// 驾车路线
	@Override
	public void onGetDrivingRouteResult(DrivingRouteResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("onGetDrivingRouteResult:抱歉，未找到结果"));
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			// 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
			// result.getSuggestAddrInfo()
			return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			if (result.getRouteLines().size() >= 1) {
				DrivingRouteOverlay overlay = new DrivingRouteOverlay(baiduMap);
//				baiduMap.setOnMarkerClickListener(overlay);
				overlay.setData(result.getRouteLines().get(0));
				overlay.addToMap();
				overlay.zoomToSpan();
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("DrivingRouteResult 结果数<0"));
				return;
			}
		}
	}

	// 公交路线
	@Override
	public void onGetTransitRouteResult(TransitRouteResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("onGetTransitRouteResult:抱歉，未找到结果"));
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			// 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
			// result.getSuggestAddrInfo()
			return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			if (result.getRouteLines().size() >= 1) {
				TransitRouteOverlay overlay = new TransitRouteOverlay(baiduMap);
//				baiduMap.setOnMarkerClickListener(overlay);
				overlay.setData(result.getRouteLines().get(0));
				overlay.addToMap();
				overlay.zoomToSpan();
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("TransitRouteResult 结果数<0"));
				return;
			}
		}
	}

	// 步行路线
	@Override
	public void onGetWalkingRouteResult(WalkingRouteResult result) {
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("onGetWalkingRouteResult:抱歉，未找到结果"));
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			// 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
			// result.getSuggestAddrInfo()
			return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			if (result.getRouteLines().size() >= 1) {
				WalkingRouteOverlay overlay = new WalkingRouteOverlay(baiduMap);
//				baiduMap.setOnMarkerClickListener(overlay);
				overlay.setData(result.getRouteLines().get(0));
				overlay.addToMap();
				overlay.zoomToSpan();
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView_View", new Exception("WalkingRouteResult 结果数<0"));
				return;
			}
		}
	}

	@Override
	public void onGetOfflineMapState(int type, int state) {

		switch (type) {
		case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
			// 离线地图下载更新事件类型
			MKOLUpdateElement update = mKOfflineMap.getUpdateInfo(state);
			fireOfflineMapEvent(update.cityID, update.cityName, update.ratio);
			break;
		case MKOfflineMap.TYPE_NEW_OFFLINE:
			// 有新离线地图安装
			break;
		case MKOfflineMap.TYPE_VER_UPDATE:
			// 版本更新提示
			break;
		}

	}

	private void fireOfflineMapEvent(int cityId, String cityName, int ratio) {
		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("cityId", cityId);
			jsonObject.put("cityName", cityName);
			jsonObject.put("ratio", ratio);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		_invokeResult.setResultNode(jsonObject);
		model.getEventCenter().fireEvent("download", _invokeResult);

	}

	@Override
	public void pauseDownload(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		int _cityId = DoJsonHelper.getInt(_dictParas, "cityID", 0);
		boolean _pauseTag = mKOfflineMap.pause(_cityId);
		_invokeResult.setResultBoolean(_pauseTag);
	}

	@Override
	public void removeDownload(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		int _cityId = DoJsonHelper.getInt(_dictParas, "cityID", 0);
		boolean _removeTag = mKOfflineMap.remove(_cityId);
		_invokeResult.setResultBoolean(_removeTag);
	}

	@Override
	public void getHotCityList(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
		ArrayList<MKOLSearchRecord> offlineCityList = mKOfflineMap.getHotCityList();
		JSONArray ja = new JSONArray();
		for (MKOLSearchRecord record : offlineCityList) {
			JSONObject _obj = new JSONObject();
			_obj.put("cityID", record.cityID);
			_obj.put("cityName", record.cityName);
			_obj.put("size", record.size);
			ja.put(_obj);
		}
		_invokeResult.setResultArray(ja);
		_scriptEngine.callback(_callbackFuncName, _invokeResult);
	}

	@Override
	public void startDownload(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
		int _cityId = DoJsonHelper.getInt(_dictParas, "cityID", 0);
		boolean _startTag = mKOfflineMap.start(_cityId);
		_invokeResult.setResultBoolean(_startTag);
		_scriptEngine.callback(_callbackFuncName, _invokeResult);
	}
}
