package doext.implement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.InfoWindow.OnInfoWindowClickListener;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiBoundSearchOption;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
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
import core.object.DoUIModule;
import doext.define.do_BaiduMapView_IMethod;
import doext.define.do_BaiduMapView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_Label_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_BaiduMapView_View extends FrameLayout implements DoIUIModuleView, do_BaiduMapView_IMethod, DoIModuleTypeID {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_BaiduMapView_MAbstract model;
	private MapView mapView;
	private BaiduMap baiduMap;
	private Map<String, Marker> overlays;
	private Context mContext;
	private String popWindowId;

	public do_BaiduMapView_View(Context context) {
		super(context);
		SDKInitializer.initialize(context.getApplicationContext());
		this.mContext = context;
		initView(context);
	}

	/**
	 * 初始化组件
	 * 
	 * @param context
	 */
	private void initView(Context context) {
		mapView = new MapView(context);
		mapView.showZoomControls(false);
		FrameLayout.LayoutParams fParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		this.addView(mapView, fParams);
		baiduMap = mapView.getMap();
		baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
		baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(10).build()));
		overlays = new HashMap<String, Marker>();
		baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {

			@Override
			public boolean onMarkerClick(Marker arg0) {
				// 显示弹窗
				Button _pop = new Button(mContext);
				final String id = arg0.getExtraInfo().getString("id");
				String info = arg0.getExtraInfo().getString("info");
				if (info != null)
					_pop.setText(info);
				_pop.setTextSize(13f);
				int _popupId = DoResourcesHelper.getIdentifier("popup", "drawable", do_BaiduMapView_View.this);
				_pop.setBackgroundResource(_popupId);
				_pop.setGravity(Gravity.CENTER);

				InfoWindow mInfoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(_pop), arg0.getPosition(), -47, new OnInfoWindowClickListener() {
					public void onInfoWindowClick() {
						doBaiduMapView_TouchMarker(id);
					}
				});
				popWindowId = id;
				baiduMap.showInfoWindow(mInfoWindow);

				// 标记点击事件回调
				doBaiduMapView_TouchMarker(id);

				return true;
			}
		});
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {

		this.model = (do_BaiduMapView_MAbstract) _doUIModule;
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
			int _zoomLevel = DoTextHelper.strToInt(_changedValues.get("zoomLevel"), 10);
			baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(_zoomLevel).build()));
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
		return false;
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
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
		overlays.clear();
		baiduMap.clear();
		mapView.onDestroy();
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
			// 定义地图状态
			MapStatus mMapStatus = new MapStatus.Builder().target(cenpt).build();
			// 定义MapStatusUpdate对象，以便描述地图状态将要发生的变化

			MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
			// 改变地图状态
			baiduMap.setMapStatus(mMapStatusUpdate);
			_invokeResult.setResultBoolean(true);
		} else {
			_invokeResult.setResultBoolean(false);
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
			JSONArray dataArray = (JSONArray) _dictParas.get("data");
			for (int i = 0; i < dataArray.length(); i++) {
				JSONObject childData = dataArray.getJSONObject(i);
				String id = DoJsonHelper.getString(childData, "id", "");
				Double latitude = DoJsonHelper.getDouble(childData, "latitude", 39.915174);
				Double longitude = DoJsonHelper.getDouble(childData, "longitude", 116.403901);
				String url = DoJsonHelper.getString(childData, "url", "");
				String info = DoJsonHelper.getString(childData, "info", "");
				LatLng latLng = new LatLng(latitude, longitude);
				Bundle bundle = new Bundle();
				bundle.putString("id", id);
				bundle.putString("info", info);
				// 构建Marker图标
				BitmapDescriptor bitmap = BitmapDescriptorFactory.fromBitmap(getLocalBitmap(url));
				// 构建MarkerOption，用于在地图上添加Marker
				OverlayOptions option = new MarkerOptions().position(latLng).icon(bitmap).title(id).zIndex(6).extraInfo(bundle).perspective(true);
				// 在地图上添加Marker，并显示
				Marker marker = (Marker) baiduMap.addOverlay(option);
				overlays.put(id, marker);
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
		JSONArray dataArray = DoJsonHelper.getJSONArray(_dictParas, "ids");
		for (int i = 0; i < dataArray.length(); i++) {
			if (overlays.containsKey(dataArray.get(i))) {
				overlays.get(dataArray.get(i)).remove();
				overlays.remove(dataArray.get(i));
				if (popWindowId != null && dataArray.get(i).equals(popWindowId)) {
					baiduMap.hideInfoWindow();
				}
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduMapView removeMarker \r\n", new Exception("标记id:" + dataArray.get(i) + "不存在"));
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

	private void doBaiduMapView_TouchMarker(String id) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		_invokeResult.setResultText(id);
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
}
