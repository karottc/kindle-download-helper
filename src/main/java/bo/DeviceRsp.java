package bo;

import java.util.List;

/**
 * @author karottc@gmail.com
 * @date 2022-06-10 22:17
 */
public class DeviceRsp {
    public GetDevicesAll GetDevices;

    public GetDevicesAll getGetDevices() {
        return GetDevices;
    }

    public void setGetDevices(GetDevicesAll getDevices) {
        GetDevices = getDevices;
    }

    public static class GetDevicesAll {
        public List<DeviceItem> devices;

        public List<DeviceItem> getDevices() {
            return devices;
        }

        public void setDevices(List<DeviceItem> devices) {
            this.devices = devices;
        }
    }

}
