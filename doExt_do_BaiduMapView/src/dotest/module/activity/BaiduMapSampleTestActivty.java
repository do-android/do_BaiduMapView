package dotest.module.activity;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.doext.module.activity.R;

import core.DoServiceContainer;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.implement.do_BaiduMapView_Model;
import doext.implement.do_BaiduMapView_View;
import dotest.module.activity.DoTestActivity;
import dotest.module.frame.debug.DoPage;
import dotest.module.frame.debug.DoService;
/**
 * webview组件测试样例
 */
@SuppressLint("ShowToast")
public class BaiduMapSampleTestActivty extends DoTestActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void initModuleModel() throws Exception {
		this.model = new do_BaiduMapView_Model();
	}
	
	@Override
	protected void initUIView() throws Exception {
		do_BaiduMapView_View view = new do_BaiduMapView_View(this);
        DoPage _doPage = new DoPage();
        ((DoUIModule)this.model).setCurrentUIModuleView(view);
        ((DoUIModule)this.model).setCurrentPage(_doPage);
        view.loadView((DoUIModule)this.model);
        LinearLayout uiview = (LinearLayout)findViewById(R.id.uiview);
        uiview.addView(view);
	}

	@Override
	public void doTestProperties(View view) {
		 DoService.setPropertyValue(this.model, "zoomLevel", "15");
	}

	@Override
	protected void doTestSyncMethod() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("请输入要测试方法的序号");
		final EditText _editText = new EditText(this);
		_editText.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(_editText);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String resultNum = _editText.getText().toString().trim();
				if(resultNum==null||"".equals(resultNum))
					return;
				
				int result = Integer.parseInt(resultNum);
				switch (result) {
				case 1:
					 //设置地图中心点坐标
					 Map<String, Object> centerXy = new HashMap<String,Object>();
					 centerXy.put("latitude", "40.915174");
					 centerXy.put("longitude", "117.403901");
					 DoService.syncMethod(model, "setCenter", centerXy);
					break;
				case 2:
					//在地图上添加一组标记
					Map<String, Object> markers = new HashMap<String, Object>();
					JSONArray data = getMarkerStrs();
					markers.put("data", data);
					DoService.syncMethod(model, "addMarkers", markers);
					break;
				case 3:
					//在地图上删除一组标记
					Map<String, Object> remove_markers = new HashMap<String, Object>();
					JSONArray jsonArray = new JSONArray();
					jsonArray.put("0_test_id_");
					jsonArray.put("2_test_id_");
					
					remove_markers.put("ids", jsonArray);
					DoService.syncMethod(model, "removeMarker", remove_markers);
					break;
					
				case 4:
					//移除所有的标记
					DoService.syncMethod(model, "removeAll", null);
					break;
				default:
					Toast.makeText(getApplicationContext(), "输入序号有误", Toast.LENGTH_SHORT).show();
					break;
				}
			}
		});
		builder.setNegativeButton("取消", null);
		builder.show();
	}

	@Override
	protected void doTestAsyncMethod() {
	}

	@Override
	protected void onEvent() {
		Toast.makeText(getApplicationContext(), "注册touchMarker事件回调监听", Toast.LENGTH_SHORT).show();
		DoService.subscribeEvent(this.model, "touchMarker", new DoService.EventCallBack() {
			@Override
			public void eventCallBack(String _data) {
				DoServiceContainer.getLogEngine().writeDebug("事件回调：" + _data);
				Toast.makeText(getApplicationContext(), _data, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void doTestFireEvent(View view) {
		DoInvokeResult invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		this.model.getEventCenter().fireEvent("_messageName", invokeResult);
	}
	
	private JSONArray getMarkerStrs(){
		
		JSONArray jsonArray = null;
		try {
			
			jsonArray = new JSONArray();
			for(int i=0;i<3;i++){
				JSONObject stoneObject = new JSONObject();  
                stoneObject.put("id", i+"_test_id_");  
                stoneObject.put("latitude", 39.915174+i*0.1+"");  
                stoneObject.put("longitude", 116.403901+i*0.1+"");  
                stoneObject.put("url", "/storage/emulated/0/picture/icon_marka.png");
                stoneObject.put("info", "描述信息"+i);
                jsonArray.put(stoneObject);
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonArray;
	}
}
