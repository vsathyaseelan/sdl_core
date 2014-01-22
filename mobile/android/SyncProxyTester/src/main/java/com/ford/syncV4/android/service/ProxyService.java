package com.ford.syncV4.android.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.ford.syncV4.android.R;
import com.ford.syncV4.android.activity.SyncProxyTester;
import com.ford.syncV4.android.adapters.logAdapter;
import com.ford.syncV4.android.constants.Const;
import com.ford.syncV4.android.constants.FlavorConst;
import com.ford.syncV4.android.module.ModuleTest;
import com.ford.syncV4.android.policies.PoliciesTest;
import com.ford.syncV4.android.policies.PoliciesTesterActivity;
import com.ford.syncV4.android.receivers.SyncReceiver;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.protocol.enums.ServiceType;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALMTesting;
import com.ford.syncV4.proxy.rpc.AddCommand;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertManeuverResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.MenuParams;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnKeyboardInput;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnSyncPData;
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
import com.ford.syncV4.proxy.rpc.OnTouchEvent;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFile;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.RegisterAppInterface;
import com.ford.syncV4.proxy.rpc.RegisterAppInterfaceResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIcon;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.Show;
import com.ford.syncV4.proxy.rpc.ShowConstantTBTResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButton;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.SyncMsgVersion;
import com.ford.syncV4.proxy.rpc.SyncPDataResponse;
import com.ford.syncV4.proxy.rpc.UnregisterAppInterfaceResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.UpdateTurnListResponse;
import com.ford.syncV4.proxy.rpc.enums.AppHMIType;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.FileType;
import com.ford.syncV4.proxy.rpc.enums.HMILevel;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.transport.BTTransportConfig;
import com.ford.syncV4.transport.BaseTransportConfig;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.ford.syncV4.transport.usb.USBTransportConfig;
import com.ford.syncV4.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Vector;

public class ProxyService extends Service implements IProxyListenerALMTesting {
    static final String TAG = "SyncProxyTester";

    private static final String APPID_BT = FlavorConst.APPID_BT;
    private static final String APPID_TCP = FlavorConst.APPID_TCP;
    private static final String APPID_USB = FlavorConst.APPID_USB;
    public static final int HEARTBEAT_INTERVAL = 5000;
    public static final int HEARTBEAT_INTERVAL_MAX = Integer.MAX_VALUE;
    private Integer autoIncCorrId = 1;

    private static final String ICON_SYNC_FILENAME = "icon.png";
    private static final String ICON_FILENAME_SUFFIX = ".png";

    private static final int XML_TEST_COMMAND = 100;
    private static final int POLICIES_TEST_COMMAND = 101;

    private static SyncProxyTester _mainInstance;
    private static ProxyService _instance;
    private static SyncProxyALM _syncProxy;
    private static logAdapter _msgAdapter;
    private ModuleTest _testerMain;
    private MediaPlayer embeddedAudioPlayer;
    private Boolean playingAudio = false;
    protected SyncReceiver mediaButtonReceiver;

    private boolean firstHMIStatusChange = true;
    private HMILevel prevHMILevel = HMILevel.HMI_NONE;

    private static boolean waitingForResponse = false;
    private ProxyServiceEvent mServiceDestroyEvent;

    private int awaitingPutFileResponseCorrelationID;


    public void onCreate() {
        super.onCreate();

        IntentFilter mediaIntentFilter = new IntentFilter();
        mediaIntentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);

        mediaButtonReceiver = new SyncReceiver();
        registerReceiver(mediaButtonReceiver, mediaIntentFilter);

        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.onCreate()", Log.INFO);
        else Log.i(TAG, "ProxyService.onCreate()");

        _instance = this;
    }

    public void showLockMain() {
        if (SyncProxyTester.getInstance() == null) {
            Intent i = new Intent(this, SyncProxyTester.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);

            // quite a few things downstream depend on the main activity and its
            // fields being alive, so wait for a while here
            int numTries = 9;
            while ((SyncProxyTester.getInstance() == null) && (numTries-- >= 0)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "created " + SyncProxyTester.getInstance());
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.onStartCommand()", Log.INFO);
        else Log.i(TAG, "ProxyService.onStartCommand()");

        startProxyIfNetworkConnected();

        setCurrentActivity(SyncProxyTester.getInstance());

        return START_STICKY;
    }

    /**
     * Function checks if WiFi enabled.
     * Manifest permission is required:
     * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
     *
     * @return true if enabled
     */
    private boolean hasWiFiConnection() {
        boolean result = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            if (netInfo != null) {
                for (NetworkInfo ni : netInfo) {
                    if (ni.getTypeName().equalsIgnoreCase("WIFI")) {
                        Log.d(TAG, ni.getTypeName());
                        if (ni.isConnected()) {
                            Log.d(TAG,
                                    "ProxyService().hasWiFiConnection(): wifi conncetion found");
                            result = true;
                        }
                    }
                }
            }
        }
        return result;
    }

    private void startProxyIfNetworkConnected() {
        final SharedPreferences prefs = getSharedPreferences(Const.PREFS_NAME,
                MODE_PRIVATE);
        final int transportType = prefs.getInt(
                Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
                Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);

        if (transportType == Const.Transport.KEY_BLUETOOTH) {
            Log.d(TAG, "ProxyService. onStartCommand(). Transport = Bluetooth.");
            BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBtAdapter != null) {
                if (mBtAdapter.isEnabled()) {
                    startProxy();
                }
            }
        } else {
            //TODO: This code is commented out for simulator purposes
            /*
            Log.d(TAG, "ProxyService. onStartCommand(). Transport = WiFi.");
			if (hasWiFiConnection() == true) {
				Log.d(TAG, "ProxyService. onStartCommand(). WiFi enabled.");
				startProxy();
			} else {
				Log.w(TAG,
						"ProxyService. onStartCommand(). WiFi is not enabled.");
			}
			*/
            startProxy();
        }
    }

    public void startProxy() {
        SyncProxyALM.enableDebugTool();

        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.startProxy()", true);
        else Log.i(TAG, "ProxyService.startProxy()");

        if (_syncProxy == null) {
            try {
                SharedPreferences settings = getSharedPreferences(
                        Const.PREFS_NAME, 0);
                boolean isMediaApp = settings.getBoolean(
                        Const.PREFS_KEY_ISMEDIAAPP,
                        Const.PREFS_DEFAULT_ISMEDIAAPP);
                boolean isNaviApp = settings.getBoolean(
                        Const.PREFS_KEY_ISNAVIAPP,
                        Const.PREFS_DEFAULT_ISNAVIAPP);
                int versionNumber = getCurrentProtocolVersion();
                String appName = settings.getString(Const.PREFS_KEY_APPNAME,
                        Const.PREFS_DEFAULT_APPNAME);
                Language lang = Language.valueOf(settings.getString(
                        Const.PREFS_KEY_LANG, Const.PREFS_DEFAULT_LANG));
                Language hmiLang = Language.valueOf(settings.getString(
                        Const.PREFS_KEY_HMILANG, Const.PREFS_DEFAULT_HMILANG));
                Log.i(TAG, "Using protocol version " + versionNumber);
                int transportType = settings.getInt(
                        Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
                        Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);
                String ipAddress = settings.getString(
                        Const.Transport.PREFS_KEY_TRANSPORT_IP,
                        Const.Transport.PREFS_DEFAULT_TRANSPORT_IP);
                int tcpPort = settings.getInt(
                        Const.Transport.PREFS_KEY_TRANSPORT_PORT,
                        Const.Transport.PREFS_DEFAULT_TRANSPORT_PORT);
                boolean mIsNSD = settings.getBoolean(Const.Transport.PREFS_KEY_IS_NSD, false);

                SyncMsgVersion syncMsgVersion = new SyncMsgVersion();
                syncMsgVersion.setMajorVersion(2);
                syncMsgVersion.setMinorVersion(2);
                Vector<AppHMIType> appHMITypes = createAppTypeVector(isNaviApp);
                String appID = null;
                BaseTransportConfig config = null;
                switch (transportType) {
                    case Const.Transport.KEY_BLUETOOTH:
                        config = new BTTransportConfig();
                        appID = APPID_BT;
                        break;

                    case Const.Transport.KEY_TCP:
                        config = new TCPTransportConfig(tcpPort, ipAddress);
                        ((TCPTransportConfig) config).setIsNSD(mIsNSD);
                        ((TCPTransportConfig) config).setApplicationContext(this);
                        appID = APPID_TCP;
                        break;

                    case Const.Transport.KEY_USB:
                        config = new USBTransportConfig(getApplicationContext());
                        appID = APPID_USB;
                        break;
                }

                _syncProxy = new SyncProxyALM(this,
                        /*sync proxy configuration resources*/null,
                        /*enable advanced lifecycle management true,*/
                        appName,
                        /*ngn media app*/null,
                        /*vr synonyms*/null,
                        /*is media app*/isMediaApp, appHMITypes,
                        syncMsgVersion,
                        /*language desired*/lang,
                        /*HMI Display Language Desired*/hmiLang,
                        appID,
                        /*autoActivateID*/null,
                        /*callbackToUIThre1ad*/ false,
                        /*preRegister*/ false,
                        versionNumber,
                        config);
            } catch (SyncException e) {
                Log.e(TAG, e.toString());
                //error creating proxy, returned proxy = null
                if (_syncProxy == null) {
                    stopSelf();
                }
            }
        }
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null)
            _msgAdapter.logMessage("ProxyService.startProxy() returning", Log.INFO);
        else Log.i(TAG, "ProxyService.startProxy() returning");
    }

    private Vector<AppHMIType> createAppTypeVector(boolean naviApp) {
        if (naviApp) {
            Vector<AppHMIType> vector = new Vector<AppHMIType>();
            vector.add(AppHMIType.NAVIGATION);
            return vector;
        }
        return null;
    }

    private int getCurrentProtocolVersion() {
        return Const.PROTOCOL_VERSION_2;
    }

    private boolean getAutoSetAppIconFlag() {
        return getSharedPreferences(Const.PREFS_NAME, 0).getBoolean(
                Const.PREFS_KEY_AUTOSETAPPICON,
                Const.PREFS_DEFAULT_AUTOSETAPPICON);
    }

    public void onDestroy() {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.onDestroy()", Log.INFO);
        else Log.i(TAG, "ProxyService.onDestroy()");

        // In case service is destroying by System
        if (mServiceDestroyEvent == null) {
            disposeSyncProxy();
        }
        mServiceDestroyEvent = null;
        _instance = null;
        if (embeddedAudioPlayer != null) {
            embeddedAudioPlayer.release();
        }
        unregisterReceiver(mediaButtonReceiver);
        super.onDestroy();
    }

    public void destroyService(ProxyServiceEvent serviceDestroyEvent) {
        mServiceDestroyEvent = serviceDestroyEvent;
        disposeSyncProxy();
    }

    private void disposeSyncProxy() {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null)
            _msgAdapter.logMessage("ProxyService.disposeSyncProxy()", Log.INFO);
        else Log.i(TAG, "ProxyService.disposeSyncProxy()");

        if (_syncProxy != null) {
            try {
                _syncProxy.dispose();
            } catch (SyncException e) {
                Log.e(TAG, e.toString());
                if (mServiceDestroyEvent != null) {
                    mServiceDestroyEvent.onDisposeError();
                }
            }
            _syncProxy = null;
        }
    }

    private void initialize() {
        playingAudio = true;
        playAnnoyingRepetitiveAudio();

        try {
            show("Sync Proxy", "Tester");
        } catch (SyncException e) {
            if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
            if (_msgAdapter != null)
                _msgAdapter.logMessage("Error sending show", Log.ERROR, e, true);
            else Log.e(TAG, "Error sending show", e);
        }

        try {
            subscribeToButton(ButtonName.OK);
            subscribeToButton(ButtonName.SEEKLEFT);
            subscribeToButton(ButtonName.SEEKRIGHT);
            subscribeToButton(ButtonName.TUNEUP);
            subscribeToButton(ButtonName.TUNEDOWN);
            Vector<ButtonName> buttons = new Vector<ButtonName>(Arrays.asList(new ButtonName[]{
                    ButtonName.OK, ButtonName.SEEKLEFT, ButtonName.SEEKRIGHT, ButtonName.TUNEUP,
                    ButtonName.TUNEDOWN}));
            SyncProxyTester.getInstance().buttonsSubscribed(buttons);
        } catch (SyncException e) {
            if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
            if (_msgAdapter != null)
                _msgAdapter.logMessage("Error subscribing to buttons", Log.ERROR, e, true);
            else Log.e(TAG, "Error subscribing to buttons", e);
        }


        try {
            addCommand(XML_TEST_COMMAND, new Vector<String>(Arrays.asList(new String[]{"XML Test", "XML"})), "XML Test");
            addCommand(POLICIES_TEST_COMMAND, new Vector<String>(Arrays.asList(new String[]{"Policies Test", "Policies"})), "Policies Test");
        } catch (SyncException e) {
            if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
            if (_msgAdapter != null)
                _msgAdapter.logMessage("Error adding AddCommands", Log.ERROR, e, true);
            else Log.e(TAG, "Error adding AddCommands", e);
        }
    }

    private void setInitAppIcon() {
        PutFile putFile = new PutFile();
        putFile.setFileType(FileType.GRAPHIC_PNG);
        putFile.setSyncFileName(ICON_SYNC_FILENAME);
        putFile.setCorrelationID(nextCorrID());
        putFile.setBulkData(contentsOfResource(R.raw.fiesta));
        try {
            _msgAdapter.logMessage(putFile, true);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException ", e);
        }

        try {
            awaitingPutFileResponseCorrelationID = putFile.getCorrelationID();
            getProxyInstance().sendRPCRequest(putFile);
        } catch (SyncException e) {
            Log.e(TAG, "Error init AppIcon -> PutFile request", e);
            awaitingPutFileResponseCorrelationID = 0;
        }
    }

    private void show(String mainField1, String mainField2) throws SyncException {
        Show msg = new Show();
        msg.setCorrelationID(nextCorrID());
        msg.setMainField1(mainField1);
        msg.setMainField2(mainField2);
        _msgAdapter.logMessage(msg, true);
        _syncProxy.sendRPCRequest(msg);
    }

    private void addCommand(Integer cmdId, Vector<String> vrCommands,
                            String menuName) throws SyncException {
        AddCommand addCommand = new AddCommand();
        addCommand.setCorrelationID(nextCorrID());
        addCommand.setCmdID(cmdId);
        addCommand.setVrCommands(vrCommands);
        MenuParams menuParams = new MenuParams();
        menuParams.setMenuName(menuName);
        addCommand.setMenuParams(menuParams);
        _msgAdapter.logMessage(addCommand, true);
        _syncProxy.sendRPCRequest(addCommand);
    }

    private void subscribeToButton(ButtonName buttonName) throws SyncException {
        SubscribeButton msg = new SubscribeButton();
        msg.setCorrelationID(nextCorrID());
        msg.setButtonName(buttonName);
        _msgAdapter.logMessage(msg, true);
        _syncProxy.sendRPCRequest(msg);
    }

    public void playPauseAnnoyingRepetitiveAudio() {
        if (embeddedAudioPlayer != null && embeddedAudioPlayer.isPlaying()) {
            playingAudio = false;
            pauseAnnoyingRepetitiveAudio();
        } else {
            playingAudio = true;
            playAnnoyingRepetitiveAudio();
        }
    }

    private void playAnnoyingRepetitiveAudio() {
        if (embeddedAudioPlayer == null) {
            embeddedAudioPlayer = MediaPlayer.create(this, R.raw.arco);
            embeddedAudioPlayer.setLooping(true);
        }
        embeddedAudioPlayer.start();

        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage("Playing audio", true);
        else Log.i(TAG, "Playing audio");
    }

    public void pauseAnnoyingRepetitiveAudio() {
        if (embeddedAudioPlayer != null && embeddedAudioPlayer.isPlaying()) {
            embeddedAudioPlayer.pause();

            if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
            if (_msgAdapter != null) _msgAdapter.logMessage("Paused audio", true);
            else Log.i(TAG, "Paused audio");
        }
    }

    public static SyncProxyALM getProxyInstance() {
        return _syncProxy;
    }

    public static ProxyService getInstance() {
        return _instance;
    }

    public SyncProxyTester getCurrentActivity() {
        return _mainInstance;
    }

    public void startModuleTest() {
        _testerMain = new ModuleTest();
    }

    public static void waiting(boolean waiting) {
        waitingForResponse = waiting;
    }

    public void setCurrentActivity(SyncProxyTester currentActivity) {
        if (this._mainInstance != null) {
            this._mainInstance.finish();
            this._mainInstance = null;
        }

        this._mainInstance = currentActivity;
        // update the _msgAdapter
        _msgAdapter = SyncProxyTester.getMessageAdapter();
    }

    protected int nextCorrID() {
        autoIncCorrId++;
        return autoIncCorrId;
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);

        switch (notification.getSystemContext()) {
            case SYSCTXT_MAIN:
                break;
            case SYSCTXT_VRSESSION:
                break;
            case SYSCTXT_MENU:
                break;
            default:
                return;
        }

        switch (notification.getAudioStreamingState()) {
            case AUDIBLE:
                if (playingAudio) playAnnoyingRepetitiveAudio();
                break;
            case NOT_AUDIBLE:
                pauseAnnoyingRepetitiveAudio();
                break;
            default:
                return;
        }

        final HMILevel curHMILevel = notification.getHmiLevel();
        final Boolean appInterfaceRegistered =
                _syncProxy.getAppInterfaceRegistered();

        if ((HMILevel.HMI_NONE == curHMILevel) && appInterfaceRegistered && firstHMIStatusChange) {
            if (!isModuleTesting()) {
                setInitAppIcon();
            }
        }

        if (prevHMILevel != curHMILevel) {
            boolean hmiChange = false;
            boolean hmiFull = false;
            switch (curHMILevel) {
                case HMI_FULL:
                    hmiFull = true;
                    hmiChange = true;
                    break;
                case HMI_LIMITED:
                    hmiChange = true;
                    break;
                case HMI_BACKGROUND:
                    hmiChange = true;
                    break;
                case HMI_NONE:
                    break;
                default:
                    return;
            }
            prevHMILevel = curHMILevel;

            if (appInterfaceRegistered) {
                if (hmiFull) {
                    if (firstHMIStatusChange) {
                        showLockMain();
                        _testerMain = new ModuleTest();
                        _testerMain = ModuleTest.getModuleTestInstance();
                        initialize();
                    } else {
                        try {
                            if (!waitingForResponse && _testerMain.getThreadContext() != null) {
                                show("Sync Proxy", "Tester Ready");
                            }
                        } catch (SyncException e) {
                            if (_msgAdapter == null)
                                _msgAdapter = SyncProxyTester.getMessageAdapter();
                            if (_msgAdapter != null)
                                _msgAdapter.logMessage("Error sending show", Log.ERROR, e, true);
                            else Log.e(TAG, "Error sending show", e);
                        }
                    }
                }

                if (hmiChange && firstHMIStatusChange) {
                    firstHMIStatusChange = false;

                    try {
                        // upload turn icons
                        sendIconFromResource(R.drawable.turn_left);
                        sendIconFromResource(R.drawable.turn_right);
                        sendIconFromResource(R.drawable.turn_forward);
                        sendIconFromResource(R.drawable.action);
                    } catch (SyncException e) {
                        Log.w(TAG, "Failed to put images", e);
                    }
                }
            }
        }
    }

    /**
     * Checks and returns if the module testing is in progress.
     *
     * @return true if the module testing is in progress
     */
    private boolean isModuleTesting() {
        return waitingForResponse && _testerMain.getThreadContext() != null;
    }

    /**
     * Returns the file contents from the specified resource.
     *
     * @param resource Resource id (in res/ directory)
     * @return The resource file's contents
     */
    private byte[] contentsOfResource(int resource) {
        InputStream is = null;
        try {
            is = getResources().openRawResource(resource);
            ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());
            final int buffersize = 4096;
            final byte[] buffer = new byte[buffersize];
            int available = 0;
            while ((available = is.read(buffer)) >= 0) {
                os.write(buffer, 0, available);
            }
            return os.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Can't read icon file", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
    }

    private void sendIconFromResource(int resource) throws SyncException {
        PutFile putFile = new PutFile();
        putFile.setFileType(FileType.GRAPHIC_PNG);
        putFile.setSyncFileName(getResources().getResourceEntryName(resource)
                + ICON_FILENAME_SUFFIX);
        putFile.setCorrelationID(nextCorrID());
        putFile.setBulkData(contentsOfResource(resource));
        _msgAdapter.logMessage(putFile, true);
        getProxyInstance().sendRPCRequest(putFile);
    }

    @Override
    public void onOnCommand(OnCommand notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);

        switch (notification.getCmdID()) {
            case XML_TEST_COMMAND:
                _testerMain.restart(null);
                break;
            case POLICIES_TEST_COMMAND:
                PoliciesTest.runPoliciesTest();
                break;
            default:
                break;
        }
    }

    @Override
    public void onProxyClosed(final String info, Exception e) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage("onProxyClosed: " + info, Log.ERROR, e);
        else Log.e(TAG, "onProxyClosed: " + info, e);

        boolean wasConnected = !firstHMIStatusChange;
        firstHMIStatusChange = true;
        prevHMILevel = HMILevel.HMI_NONE;

        if (wasConnected) {
            final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
            if (mainActivity != null) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.onProxyClosed(info);
                    }
                });
            } else {
                Log.w(TAG, "mainActivity not found");
            }
        }

        if (!isModuleTesting()) {
            if (e == null) {
                return;
            }
            final SyncExceptionCause cause = ((SyncException) e).getSyncExceptionCause();
            if ((cause != SyncExceptionCause.SYNC_PROXY_CYCLED) &&
                    (cause != SyncExceptionCause.BLUETOOTH_DISABLED) &&
                    (cause != SyncExceptionCause.SYNC_REGISTRATION_ERROR)) {
                reset();
            }

            if ((SyncExceptionCause.SYNC_PROXY_CYCLED != cause) &&
                    (_msgAdapter != null)) {
                _msgAdapter.logMessage("onProxyClosed: " + info, Log.ERROR, e,
                        true);
            }
        }
    }

    public void reset() {
        if (_syncProxy == null) {
            return;
        }
        // In case we run exit() - this is a quick marker of exiting.
        if (mServiceDestroyEvent != null) {
            return;
        }
        try {
            _syncProxy.resetProxy();
        } catch (SyncException e1) {
            e1.printStackTrace();
            //something goes wrong, & the proxy returns as null, stop the service.
            //do not want a running service with a null proxy
            if (_syncProxy == null) {
                stopSelf();
            }
        }
    }

    /**
     * Restarting SyncProxyALM. For example after changing transport type
     */
    public void restart() {
        Log.i(TAG, "ProxyService.Restart SyncProxyALM.");
        disposeSyncProxy();
        startProxyIfNetworkConnected();
    }

    @Override
    public void onError(String info, Exception e) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) {
            _msgAdapter.logMessage("******onProxyError******", Log.ERROR);
            _msgAdapter.logMessage("ERROR: " + info, Log.ERROR, e);
            _msgAdapter.logMessage(info, true);
        } else {
            Log.e(TAG, "******onProxyError******");
            Log.e(TAG, "ERROR: " + info, e);
        }
    }

    /**
     * ******************************
     * * SYNC AppLink Base Callback's **
     * *******************************
     */
    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final boolean success = response.getSuccess();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onAddSubMenuResponse(success);
            }
        });

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final boolean success = response.getSuccess();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onCreateChoiceSetResponse(success);
            }
        });

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onDeleteCommandResponse(DeleteCommandResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final boolean success = response.getSuccess();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onDeleteCommandResponse(success);
            }
        });

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final boolean success = response.getSuccess();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onDeleteChoiceSetResponse(success);
            }
        });

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final boolean success = response.getSuccess();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onDeleteSubMenuResponse(success);
            }
        });

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse response) {
        Log.i("syncp", "onEncodedSyncPDataResponse: " + response.getInfo() + response.getResultCode() + response.getSuccess());
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onSpeakResponse(SpeakResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onOnDriverDistraction(OnDriverDistraction notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);
    }

    @Override
    public void onGenericResponse(GenericResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    /**
     * ******************************
     * * SYNC AppLink Soft Button Image Callback's **
     * *******************************
     */
    @Override
    public void onPutFileResponse(PutFileResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (response.getCorrelationID() == awaitingPutFileResponseCorrelationID &&
                getAutoSetAppIconFlag()) {
            SetAppIcon setAppIcon = new SetAppIcon();
            setAppIcon.setSyncFileName(ICON_SYNC_FILENAME);
            setAppIcon.setCorrelationID(nextCorrID());
            _msgAdapter.logMessage(setAppIcon, true);
            try {
                getProxyInstance().sendRPCRequest(setAppIcon);
            } catch (SyncException e) {
                Log.e(TAG, "Set InitAppIcon", e);
            }
            awaitingPutFileResponseCorrelationID = 0;
        }

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onListFilesResponse(ListFilesResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onSetAppIconResponse(SetAppIconResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);
    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);

        switch (notification.getButtonName()) {
            case OK:
                playPauseAnnoyingRepetitiveAudio();
                break;
            case SEEKLEFT:
                break;
            case SEEKRIGHT:
                break;
            case TUNEUP:
                break;
            case TUNEDOWN:
                break;
            default:
                break;
        }
    }

    /**
     * ******************************
     * * SYNC AppLink Updated Callback's **
     * *******************************
     */
    @Override
    public void onAddCommandResponse(AddCommandResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final boolean success = response.getSuccess();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onAddCommandResponse(success);
            }
        });

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onAlertResponse(AlertResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onPerformInteractionResponse(PerformInteractionResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onShowResponse(ShowResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    /**
     * ******************************
     * * SYNC AppLink New Callback's **
     * *******************************
     */
    @Override
    public void onSliderResponse(SliderResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onOnLanguageChange(OnLanguageChange notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);
    }

    /**
     * ******************************
     * * SYNC AppLink Audio Pass Thru Callback's **
     * *******************************
     */
    @Override
    public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final Result result = response.getResultCode();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onPerformAudioPassThruResponse(result);
            }
        });
    }

    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final Result result = response.getResultCode();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onEndAudioPassThruResponse(result);
            }
        });
    }

    @Override
    public void onOnAudioPassThru(OnAudioPassThru notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        final byte[] aptData = notification.getAPTData();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onAudioPassThru(aptData);
            }
        });
    }

    /**
     * ******************************
     * * SYNC AppLink Vehicle Data Callback's **
     * *******************************
     */
    @Override
    public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onUnsubscribeVehicleDataResponse(UnsubscribeVehicleDataResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onReadDIDResponse(ReadDIDResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onGetDTCsResponse(GetDTCsResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onOnVehicleData(OnVehicleData notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);
    }

    /**
     * ******************************
     * * SYNC AppLink TBT Callback's **
     * *******************************
     */
    @Override
    public void onShowConstantTBTResponse(ShowConstantTBTResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onAlertManeuverResponse(AlertManeuverResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onUpdateTurnListResponse(UpdateTurnListResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onMobileNaviStart() {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        String response = "Mobile Navi Started";
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        if (mainActivity != null) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.onMobileNaviStarted();
                }
            });
        }
    }

    @Override
    public void onMobileNavAckReceived(int frameReceivedNumber) {
        final int fNumber = frameReceivedNumber;
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        String response = "Mobile Ack Received = " + frameReceivedNumber;
        if (_msgAdapter != null) _msgAdapter.logMessage(response, false);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        if (mainActivity != null) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.onMobileNavAckReceived(fNumber);
                }
            });
        }
    }

    @Override
    public void onOnTouchEvent(OnTouchEvent notification) {
        final OnTouchEvent event = notification;
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);


        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onTouchEventReceived(event);
            }
        });

    }

    @Override
    public void onKeyboardInput(OnKeyboardInput msg) {
        final OnKeyboardInput event = msg;
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();

        if (_msgAdapter != null) _msgAdapter.logMessage(msg, true);
        else Log.i(TAG, "" + msg.toString());

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.onKeyboardInputReceived(event);
            }
        });
    }

    @Override
    public void onRegisterAppRequest(RegisterAppInterface msg) {
        Log.i(TAG, "OnRegisterAppRequest: " + msg.toString());
        Log.d("TRACE", "OnRegisterAppRequest");
        //final  RegisterAppInterface event = msg;
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(msg, true);
        else Log.i(TAG, "" + msg.toString());
    }

    @Override
    public void onAppUnregisteredAfterLanguageChange(OnLanguageChange msg) {
        Log.i(TAG, "onAppUnregisteredAfterLanguageChange " + msg.toString());
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        final String message =
                String.format("OnAppInterfaceUnregistered (LANGUAGE_CHANGE) from %s to %s",
                        msg.getLanguage(), msg.getHmiDisplayLanguage());
        if (_msgAdapter != null) _msgAdapter.logMessage(message, true);
        else Log.i(TAG, message);

        _syncProxy.resetLanguagesDesired(msg.getLanguage(),
                msg.getHmiDisplayLanguage());
    }

    @Override
    public void onProtocolServiceEnded(final ServiceType serviceType, final Byte version, final String correlationID) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        String response = "EndService Ack received; Session Type " + serviceType.getName() + "; Session ID " + version + "; Correlation ID " + correlationID;
        if (_msgAdapter != null) _msgAdapter.logMessage(response, false);
        else Log.i(TAG, "" + response);

        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        if (mainActivity != null) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.onProtocolServiceEnded(serviceType, version, correlationID);
                }
            });
        }
    }

    @Override
    public void onSessionStarted(final byte sessionID, final String correlationID) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) {
            _msgAdapter.logMessage("Session Started; currentSession id " + sessionID, true);
        } else Log.i(TAG, "Session Started; currentSession id " + sessionID);
        final SyncProxyTester mainActivity = SyncProxyTester.getInstance();
        if (mainActivity != null) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.onSesionStarted(sessionID, correlationID);
                }
            });
        }
    }

    @Override
    public void onAudioServiceStart() {
        
    }

    @Override
    public void onOnTBTClientState(OnTBTClientState notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);
    }

    /**
     * ******************************
     * * SYNC AppLink Policies Callback's **
     * *******************************
     */
    @Override
    public void onOnPermissionsChange(OnPermissionsChange notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);
    }

    EncodedSyncPDataHeader encodedSyncPDataHeaderfromGPS;

    @Override
    public void onOnEncodedSyncPData(OnEncodedSyncPData notification) {
        Log.i("syncp", "MessageType: " + notification.getMessageType());

        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);

        EncodedSyncPDataHeader encodedSyncPDataHeader;
        try {
            encodedSyncPDataHeader = EncodedSyncPDataHeader.parseEncodedSyncPDataHeader(
                    Base64.decode(notification.getData().get(0)));
        } catch (IOException e) {
            Log.e(TAG, "Can't decode base64 string", e);
            return;
        }

        if (encodedSyncPDataHeader.getServiceType() == 3 && encodedSyncPDataHeader.getCommandType() == 1) {
            writeToFile(encodedSyncPDataHeader.getPayload());

            Log.i("EncodedSyncPDataHeader", "Protocol Version: " + encodedSyncPDataHeader.getProtocolVersion());
            Log.i("EncodedSyncPDataHeader", "Response Required: " + encodedSyncPDataHeader.getResponseRequired());
            Log.i("EncodedSyncPDataHeader", "High Bandwidth: " + encodedSyncPDataHeader.getHighBandwidth());
            Log.i("EncodedSyncPDataHeader", "Signed: " + encodedSyncPDataHeader.getSigned());
            Log.i("EncodedSyncPDataHeader", "Encrypted: " + encodedSyncPDataHeader.getEncrypted());
            Log.i("EncodedSyncPDataHeader", "Payload Size: " + encodedSyncPDataHeader.getPayloadSize());
            Log.i("EncodedSyncPDataHeader", "Has ESN: " + encodedSyncPDataHeader.getHasESN());
            Log.i("EncodedSyncPDataHeader", "Service Type: " + encodedSyncPDataHeader.getServiceType());
            Log.i("EncodedSyncPDataHeader", "Command Type: " + encodedSyncPDataHeader.getCommandType());
            Log.i("EncodedSyncPDataHeader", "CPU Destination: " + encodedSyncPDataHeader.getCPUDestination());
            Log.i("EncodedSyncPDataHeader", "Encryption Key Index: " + encodedSyncPDataHeader.getEncryptionKeyIndex());

            byte[] tempESN = encodedSyncPDataHeader.getESN();
            String stringESN = "";
            for (int i = 0; i < 8; i++) stringESN += tempESN[i];
            Log.i("EncodedSyncPDataHeader", "ESN: " + stringESN);

            try {
                Log.i("EncodedSyncPDataHeader", "Module Message ID: " + encodedSyncPDataHeader.getModuleMessageID());
            } catch (Exception e) {
            }
            try {
                Log.i("EncodedSyncPDataHeader", "Server Message ID: " + encodedSyncPDataHeader.getServerMessageID());
            } catch (Exception e) {
            }
            try {
                Log.i("EncodedSyncPDataHeader", "Message Status: " + encodedSyncPDataHeader.getMessageStatus());
            } catch (Exception e) {
            }

            //create header for syncp packet
            if (encodedSyncPDataHeader.getHighBandwidth()) {
                byte[] tempIV = encodedSyncPDataHeader.getIV();
                String stringIV = "";
                for (int i = 0; i < 16; i++) stringIV += tempIV[i];
                Log.i("EncodedSyncPDataHeader", "IV: " + stringIV);

                byte[] tempPayload = encodedSyncPDataHeader.getPayload();
                String stringPayload = "";
                for (int i = 0; i < encodedSyncPDataHeader.getPayloadSize(); i++)
                    stringPayload += tempPayload[i];
                Log.i("EncodedSyncPDataHeader", "Payload: " + stringPayload);

                byte[] tempSignatureTag = encodedSyncPDataHeader.getSignatureTag();
                String stringSignatureTag = "";
                for (int i = 0; i < 16; i++) stringSignatureTag += tempSignatureTag[i];
                Log.i("EncodedSyncPDataHeader", "Signature Tag: " + stringSignatureTag);
            } else {
                byte[] tempIV = encodedSyncPDataHeader.getIV();
                String stringIV = "";
                for (int i = 0; i < 8; i++) stringIV += tempIV[i];
                Log.i("EncodedSyncPDataHeader", "IV: " + stringIV);

                byte[] tempPayload = encodedSyncPDataHeader.getPayload();
                String stringPayload = "";
                for (int i = 0; i < encodedSyncPDataHeader.getPayloadSize(); i++)
                    stringPayload += tempPayload[i];
                Log.i("EncodedSyncPDataHeader", "Payload: " + stringPayload);

                byte[] tempSignatureTag = encodedSyncPDataHeader.getSignatureTag();
                String stringSignatureTag = "";
                for (int i = 0; i < 8; i++) stringSignatureTag += tempSignatureTag[i];
                Log.i("EncodedSyncPDataHeader", "Signature Tag: " + stringSignatureTag);
            }

            encodedSyncPDataHeaderfromGPS = encodedSyncPDataHeader;
            if (_msgAdapter != null) SyncProxyTester.setESN(tempESN);
            if (PoliciesTesterActivity.getInstance() != null) {
                PoliciesTesterActivity.setESN(tempESN);
                PoliciesTesterActivity.setHeader(encodedSyncPDataHeader);
            }
        }

        if (encodedSyncPDataHeader.getServiceType() == 7) {
            writeToFile(encodedSyncPDataHeader.getPayload());
        }
    }

    public void writeToFile(Object writeME) {
        //FileInputStream fin;
        try {
            //fin = new FileInputStream("/sdcard/" + "policiesResults.txt");
            //InputStreamReader isr = new InputStreamReader(fin);
            //String outFile = "/sdcard/" + mChosenFile.substring(0, mChosenFile.length() - 4) + ".csv";
            //String outFile = "/sdcard/" + "policiesResults.txt";
            String outFile = Environment.getExternalStorageDirectory().getPath() + "/policiesResults.txt";
            File out = new File(outFile);
            FileWriter writer = new FileWriter(out);
            writer.flush();

            //writer.write("yay" + "\n");
            writer.write(writeME.toString());
            //writer.write("double yay" + "\n");


            writer.close();
        } catch (FileNotFoundException e) {
            Log.i("syncp", "FileNotFoundException: " + e);
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.i("syncp", "IOException: " + e);
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage("Service on Bind");
        else Log.i(TAG, "Service on Bind");
        return new Binder();
    }

    @Override
    public void onRegisterAppInterfaceResponse(
            RegisterAppInterfaceResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }
    }

    @Override
    public void onUnregisterAppInterfaceResponse(UnregisterAppInterfaceResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
        }

        if (mServiceDestroyEvent != null) {
            mServiceDestroyEvent.onDisposeComplete();
        }
    }

    @Override
    public void onSyncPDataResponse(SyncPDataResponse response) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
        else Log.i(TAG, "" + response);

        if (isModuleTesting()) {
            ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
            synchronized (_testerMain.getThreadContext()) {
                _testerMain.getThreadContext().notify();
            }
            ;
        }

    }

    @Override
    public void onOnSyncPData(OnSyncPData notification) {
        if (_msgAdapter == null) _msgAdapter = SyncProxyTester.getMessageAdapter();
        if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
        else Log.i(TAG, "" + notification);
    }
}