package doext.define;

import core.object.DoUIModule;
import core.object.DoProperty;
import core.object.DoProperty.PropertyDataType;


public abstract class do_BaiduMapView_MAbstract extends DoUIModule{

	protected do_BaiduMapView_MAbstract() throws Exception {
		super();
	}
	
	/**
	 * 初始化
	 */
	@Override
	public void onInit() throws Exception{
        super.onInit();
        //注册属性
		this.registProperty(new DoProperty("zoomLevel", PropertyDataType.String, "10", false));
		this.registProperty(new DoProperty("mapType", PropertyDataType.String, "standard", false));
	}
}