package cn.org.bjca.fingerbio.finger;

import android.app.Activity;

import com.IDWORLD.LAPI;

/*************************************************************************************************
 * <pre>
 * @包路径： cn.org.bjca.trust.hospital.finger
 * @版权所有： 北京数字认证股份有限公司 (C) 2020
 *
 * @类描述:
 * @版本: V4.0.0
 * @作者 daizhenhong
 * @创建时间 2020-06-22 15:20
 *
 * @修改记录：
-----------------------------------------------------------------------------------------------
----------- 时间      |   修改人    |     修改的方法       |         修改描述   ---------------
-----------------------------------------------------------------------------------------------
</pre>
 ************************************************************************************************/
public class FingerManage {

    private static LAPI mLApi;

    public static LAPI getLApi(Activity activity) {
        if (mLApi == null) {
            synchronized (FingerManage.class) {
                if (mLApi == null) {
                    mLApi = new LAPI(activity);
                }
            }
        }
        return mLApi;
    }


    public static boolean enableGetFingerImage(Activity activity) {
        int result = getLApi(activity).OpenDeviceEx();
        if (result != 0) {
            getLApi(activity).CloseDeviceEx(result);
        }
        return result != 0;
    }
}
