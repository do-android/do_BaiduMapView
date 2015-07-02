package doext.define;

/**
 * 声明自定义扩展组件方法
 */
public interface do_BaiduMapView_IMethod {
	void addMarkers(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void removeAll(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void removeMarker(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
	void setCenter(JSONObject _dictParas,DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception ;
}