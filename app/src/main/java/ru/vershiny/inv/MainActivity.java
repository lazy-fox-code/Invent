package ru.vershiny.inv;
//ver 21/09/2016

//Модули приложения
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

//управление интерфейсом
import android.view.View;
import android.view.View.OnClickListener;
//меню
import android.view.Menu;
import android.view.MenuItem;

//диалоги
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

//элементы интерфейса
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

//управление камерой
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

//распознавание штрихкодов
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

//web-запросы
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

//асинхронные запросы
import android.os.AsyncTask;

//разбор web-ответов
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//массив списка
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//вывод в отладчик
import android.util.Log;

public class MainActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    TextView firm, mx, name, prodname, roleact; //вывод фирмы,места,имени,товара,роли
    EditText ScanText, qty; //ввод кода,количества
    Button CfcButton, ScanButton, SelectButton, SaveButton; //кнопки коэфф,скан,запрос,сохранить

    SharedPreferences sPref; //настройки пользователя

    ImageScanner scanner;
    int bcdl; //длина штрихкода
    char[] barcodeArray; //массив штрихкода
    String barcode = "";    //чистый штрихкод
    String sbarcode = "";    //записанный штрихкод
    String pref = "";       //префикс штрихкода
    String roles = "";      //роль
    String rls = "";        //знак роли
    String firmcode = "";   //код фирмы
    String firmname = "";   //фирма
    String mxcode = "";     //код места
    String mxname = "";     //место
    String namecode = "";   //код имени
    String famname = "";    //имя
    String tovar = "";      //товар
    String prodid = "";     //код товара (prodcode в firebird)
    String cfcv = "";       //коэффициент на кнопку
    String vqty = "";       //количество на кнопку
    String cfcstr = "";     //строка списка коэффициентов
    String cfcarr[] = null; //массив строк коэффициента
    String rt = "";         //результат записи в БД
    String server = "";     //путь к web-серверу

    private static final String mxPID = "mpid"; //id тов. табл.части
    private static final String mxTOV = "mtov"; //товары табл.части
    private static final String mxQTY = "mqty"; //кол-во табл.части

    ArrayList<HashMap<String, Object>> mxList; //массив табл.части
    public ListView listView;                  //табл.часть

    String deltov;
    String delpid;

    private boolean barcodeScanned = false;     //флаг распознавания
    private boolean previewing = true;          //флаг сканирования
    boolean scPrep = false;                     //фдаг подготовки камеры
    boolean bct = true;                         //флаг проверки сканирования

    private static final String TAG = "myLogs"; //фильтр отладки

    static {
        System.loadLibrary("iconv");
    }

    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
            ScanButton = (Button) findViewById(R.id.ScanButton);
            SelectButton = (Button) findViewById(R.id.SelectButton);
            CfcButton = (Button) findViewById(R.id.CfcButton);
            SaveButton = (Button) findViewById(R.id.SaveButton);
            ScanText = (EditText) findViewById(R.id.ScanText);
            qty = (EditText) findViewById(R.id.qty);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); //скроем клавиатуру
        ScanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                scanning();
            }
        }); //обработаем скан
        SelectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                select();
            }
        }); //обработаем запрос
        SaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                save();
            }
        });     //обработаем запись
        CfcButton.setOnClickListener(new OnClickListener() { //Диалог выбора коэффициента
            public void onClick(View v) {
                showDialog(1);

            }
        });//обработаем ед
        UpdateList();
    }

    void UpdateList(){ //Список записанных товаров
        Log.d(TAG, "UpdateList");
        mxList = new ArrayList<HashMap<String, Object>>();
        SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, mxList, R.layout.list,
                new String[]{mxPID, mxTOV, mxQTY}, new int[]{R.id.text3, R.id.text1, R.id.text2});
        listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.smoothScrollToPosition(mxList.size());
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) { //вызываем апдейт удаления строки
                    String del[] = listView.getItemAtPosition(position).toString().split("=");
                    deltov = del[2].replace(", mpid", "");
                    delpid = del[3].replace("}", "");
                    Log.d(TAG, "Выбрали из списка:" + listView.getItemAtPosition(position).toString());
                    if (roles.equals("inv")) showDialog(2);
                    return true;
                }
            });
    }
    void loadPref() { //Загрузка параметров
        Log.d(TAG, "loadPref");
        sPref = getPreferences(MODE_PRIVATE);
        roles = sPref.getString("ROLES", "");
        rls = sPref.getString("RLS", "");
        firmcode = sPref.getString("FIRMCODE", "");
        firmname = sPref.getString("FIRMNAME", "");
        namecode = sPref.getString("NAMECODE", "");
        famname = sPref.getString("FAMNAME", "");
        if (roles.equals("")) roles = "inv";
        if (rls.equals("")) rls = "+";
        server = getString(R.string.webserver);
        firm = (TextView) findViewById(R.id.firm);
        firm.setText(firmname);
        name = (TextView) findViewById(R.id.name);
        name.setText(famname);
        roleact = (TextView) findViewById(R.id.roleact);
        roleact.setText(rls);
        if (roles.equals("kas")) Toast.makeText(this, "Кассир", Toast.LENGTH_SHORT).show();
        if (roles.equals("inv")) Toast.makeText(this, "Инвентаризатор", Toast.LENGTH_SHORT).show();
    }
    void savePref() { //Сохранение параметров
        Log.d(TAG, "savePref");
        roleact = (TextView) findViewById(R.id.roleact);
        rls = roleact.getText().toString();
        sPref = getPreferences(MODE_PRIVATE);
        Editor ed = sPref.edit();
        ed.putString("ROLES", roles);
        ed.putString("RLS", rls);
        ed.putString("FIRMCODE", firmcode);
        ed.putString("FIRMNAME", firmname);
        ed.putString("NAMECODE", namecode);
        ed.putString("FAMNAME", famname);
        ed.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //Вызов меню
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //Меню переключения роли
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.roles) {
            sPref = getPreferences(MODE_PRIVATE);
            Editor ed = sPref.edit();
            roleact = (TextView) findViewById(R.id.roleact);
            rls = roleact.getText().toString();
            Log.d(TAG, "rls:" + rls);
            if (rls.equals("+")) {
                rls = "-";
                roleact.setText(rls);
                roles = "kas";
                ed.putString("RLS", rls);
                ed.putString("ROLES", roles);
                ed.commit();
                Log.d(TAG, "rls стало:" + rls);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); //скроем клавиатуру
                Toast.makeText(this, "Кассир", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (rls.equals("-")) {
                rls = "+";
                roleact.setText(rls);
                roles = "inv";
                ed.putString("RLS", rls);
                ed.putString("ROLES", roles);
                ed.commit();
                Log.d(TAG, "rls стало:" + rls);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); //скроем клавиатуру
                Toast.makeText(this, "Инвентаризатор", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void scanningPrepare() { //Подготовка модуля сканирования
        Log.d(TAG, "scanningPrepare");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
        scanner.setConfig(0, Config.EMIT_CHECK, 0);
        //scanner.setConfig(0, Config.ADD_CHECK, 1);
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        scPrep = true;
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    public static Camera getCameraInstance() { //Включение камеры
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
        }
        return c;
    }

    public void scanning() { //Сканирование штрихкода
        Log.d(TAG, "scanning");
                if (!scPrep) scanningPrepare();
                if (barcodeScanned) {
                    barcodeScanned = false;
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                    previewing = true;
                }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        savePref();
        super.onDestroy();
    }
    public void onStop() {
        Log.d(TAG, "onStop");
        savePref();
        super.onStop();
        previewing = false;
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.removeView(mPreview);
        releaseCamera();
    }
    public void onPause() {
        Log.d(TAG, "onPause");
        //savePref();
        super.onPause();
        previewing = false;
        releaseCamera();
    }
    private void releaseCamera() {
        Log.d(TAG, "releaseCamera");
        if (mCamera != null) {
            //previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            scPrep = false;
        }
    }
    public void onResume(){
        Log.d(TAG, "onResume");
        loadPref();

        super.onResume();
    }

    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
        }
    };

    PreviewCallback previewCb = new PreviewCallback() { //Запрос превью и получение штрихкода
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.d(TAG, "previewCbIn");
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            Size size = parameters.getPreviewSize();
            Image ibarcode = new Image(size.width, size.height, "Y800");
            ibarcode.setData(data);
            int result = scanner.scanImage(ibarcode);
            if (result != 0) {
                SymbolSet syms = scanner.getResults();
                String bcd = ""; //распознанный штрихкод
                String bcdr = "";//проверенный штрихкод
                for (Symbol sym : syms) {
                    bcd = sym.getData(); //Тут бывает два штрихкода
                    Log.d(TAG, "bcd " + bcd);
                    //Проверка что считанный шк отличается от предыдущего записанного баркода
                    if (bcd.equals(sbarcode)) {
                        Log.d(TAG, bcd + " bcd = sbarcode " + sbarcode);
                    } else {
                        bcdr = bcd;
                        Log.d(TAG, bcd + " bcd != sbarcode " + sbarcode);
                    }
                }
                if (bcdr == "") bcdr = bcd;
                if (sbarcode.equals(bcdr)) {
                    Log.d(TAG, barcode + " barcode = bcdr " + bcdr);
                    barcode = bcdr;
                    ScanText.setHint(barcode);
                    previewing = false;
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null);
                    barcodeScanned = true;
                    prodname = (TextView) findViewById(R.id.prodname);
                    prodname.setText("");
                }
                else {
                    barcode = bcdr;
                    ScanText.setHint(barcode);
                    previewing = false;
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null);
                    barcodeScanned = true;
                    select();
                }
            }
            Log.d(TAG, "previewCbOut");
        }
    };

    public void select() { //Выбор запроса штрихкода
        Log.d(TAG, "select");
        pref = "";
        barcode = ScanText.getText().toString();
        if (barcode.equals("")) barcode = ScanText.getHint().toString();
        Log.d(TAG, "ScanText = " + barcode);
        barcodeArray = barcode.toCharArray();
        bcdl = barcodeArray.length;
        if (bcdl == 12) {
            pref = barcode.substring(0, 3);
            if (pref.equals("250")) {
                if (!barcode.substring(3, 6).equals("000")) {
                    firmcode = barcode.substring(3, 6);
                    mxcode = barcode.substring(6, 9);
                    Log.d(TAG, "Запрос mx:" + server + "invfmx.php");
                    if (roles.equals("inv")) showDialog(3);
                    else new RequestMX().execute(server + "invfmx.php");
                }
                if (!barcode.substring(9, 12).equals("000") && barcode.substring(3, 9).equals("000000")) {
                        namecode = barcode.substring(9, 12);
                        Log.d(TAG, "Запрос name:" + server + "invname.php");
                        showDialog(4);
                }
            } else {
                Log.d(TAG, "Запрос товара EAN-13:" + server + "invtovar.php");
                //if (barcode.substring(0, 6).equals("000000")) barcode = barcode.substring(6, 12);
                new RequestTovar().execute(server + "invtovar.php");
            }
        } else {
            Log.d(TAG, "Запрос товара по коду:" + server + "invtovar.php");
            new RequestTovar().execute(server + "invtovar.php");
        }
    }

    class RequestMX extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                //создаем запрос на сервер
                DefaultHttpClient hc = new DefaultHttpClient();
                ResponseHandler<String> res = new BasicResponseHandler();
                //он у нас будет посылать post запрос
                HttpPost postMethod = new HttpPost(params[0]);
                //будем передавать два параметра
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                //передаем параметры из наших текстбоксов
                nameValuePairs.add(new BasicNameValuePair("firmcode", firmcode));
                nameValuePairs.add(new BasicNameValuePair("mxcode", mxcode));
                //собераем их вместе и посылаем на сервер
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                //получаем ответ от сервера
                String response = hc.execute(postMethod, res);
                Log.d(TAG, "Запрос http" + response);
                JSONURLMX(response);
            } catch (Exception e) {
                System.out.println("Exp=" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            savePref();
            firm = (TextView) findViewById(R.id.firm);
            firm.setText(firmname);
            mx = (TextView) findViewById(R.id.mx);
            if (mxname.length()==3) mx.setText(mxname);
            else {
                mxcode = "";
                mxname = "";
                mx.setText(mxname);
            }
            dialog.dismiss();
            prodname = (TextView) findViewById(R.id.prodname);
            if (mxList.size()>0 && roles.equals("inv")) prodname.setText("Шкаф записывали");
            else prodname.setText("Сканируйте товар");
                //дальше добавляем полученные параметры в наш адаптер
                SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, mxList, R.layout.list,
                        new String[]{mxPID, mxTOV, mxQTY}, new int[]{R.id.text3, R.id.text1, R.id.text2});
                //выводим в листвбю
                listView = (ListView) findViewById(R.id.list);
                listView.setAdapter(adapter);
                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                listView.smoothScrollToPosition(mxList.size());
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Загружаюсь...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show();
            if (roles.equals("inv")) mxList.clear();
            super.onPreExecute();
        }
    }
    public void JSONURLMX(String result) {
        try {
            //создали читателя json объектов и отдали ему строку - result
            JSONObject json = new JSONObject(result);
            //дальше находим вход в наш json им является ключевое слово cfg
            JSONArray urls = json.getJSONArray("cfg");
                //читаем что в себе хранит параметр firm
                firmname=(urls.getJSONObject(0).getString("firm").toString());
            Log.d(TAG, "firmname:" + firmname);
                //читаем что в себе хранит параметр mx
                mxname=(urls.getJSONObject(1).getString("mx").toString());
            Log.d(TAG, "mxname:" + mxname);
            //дальше находим вход в наш json им является ключевое слово data
            JSONArray urls1 = json.getJSONArray("mxlist");
            if (roles.equals("inv")) {
                //проходим циклом по всем нашим параметрам
                for (int m = 0; m < urls1.length(); m++) {
                    HashMap<String, Object> hm;
                    hm = new HashMap<String, Object>();
                    hm.put(mxPID, urls1.getJSONObject(m).getString("mpid").toString());
                    hm.put(mxTOV, urls1.getJSONObject(m).getString("mtov").toString());
                    hm.put(mxQTY, urls1.getJSONObject(m).getString("mqty").toString());
                    mxList.add(hm);
                }
            }

        }
        catch (JSONException e) {Log.e(TAG, "Error parsing data " + e.toString());}
    }
    class RequestName extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                //создаем запрос на сервер
                DefaultHttpClient hc = new DefaultHttpClient();
                ResponseHandler<String> res = new BasicResponseHandler();
                //он у нас будет посылать post запрос
                HttpPost postMethod = new HttpPost(params[0]);
                //будем передавать три параметра
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                //передаем параметры из наших текстбоксов
                nameValuePairs.add(new BasicNameValuePair("firmcode", firmcode));
                nameValuePairs.add(new BasicNameValuePair("namecode", namecode));
                //собераем их вместе и посылаем на сервер
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                //получаем ответ от сервера
                String response = hc.execute(postMethod, res);
                Log.d(TAG, "Запрос http" + response);
                JSONURLNAME(response);
            } catch (Exception e) {
                System.out.println("Exp=" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dialog.dismiss();
            super.onPostExecute(result);
            name = (TextView) findViewById(R.id.name);
            name.setText(famname);
            savePref();
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Загружаюсь...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show();
            super.onPreExecute();
        }
    }
    public void JSONURLNAME(String result) {
        try {
            //создали читателя json объектов и отдали ему строку - result
            JSONObject json = new JSONObject(result);
            //дальше находим вход в наш json им является ключевое слово cfg
            JSONArray urls = json.getJSONArray("cfgn");
            //читаем что в себе хранит параметр firm
            //firmcode=(urls.getJSONObject(0).getString("firm").toString());
            //Log.d(TAG, "firmcode:" + firmcode);
            //читаем что в себе хранит параметр mx
            famname=(urls.getJSONObject(0).getString("name").toString());
            Log.d(TAG, "famname:" + famname);
        }
        catch (JSONException e) {Log.e(TAG, "Error parsing data " + e.toString());}
    }
    class RequestTovar extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                //создаем запрос на сервер
                DefaultHttpClient hc = new DefaultHttpClient();
                ResponseHandler<String> res = new BasicResponseHandler();
                //он у нас будет посылать post запрос
                HttpPost postMethod = new HttpPost(params[0]);
                //будем передавать три параметра
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
                //передаем параметры из наших текстбоксов
                nameValuePairs.add(new BasicNameValuePair("firmcode", firmcode));
                nameValuePairs.add(new BasicNameValuePair("mxcode", mxcode));
                nameValuePairs.add(new BasicNameValuePair("roles", roles));
                nameValuePairs.add(new BasicNameValuePair("barcode", barcode));
                //собераем их вместе и посылаем на сервер
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                //получаем ответ от сервера
                String response = hc.execute(postMethod, res);
                Log.d(TAG, "Запрос http" + response);
                JSONURLTOVAR(response);
            } catch (Exception e) {
                System.out.println("Exp=" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dialog.dismiss();
            super.onPostExecute(result);
            ScanText.setText("");
            ScanText.setHint(barcode);
            prodname = (TextView) findViewById(R.id.prodname);
            prodname.setText(tovar);
            CfcButton = (Button) findViewById(R.id.CfcButton);
            CfcButton.setText(cfcv);
            removeDialog(1);
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Загружаюсь...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show();
            super.onPreExecute();
        }
    }
    public void JSONURLTOVAR(String result) {
        try {
            //создали читателя json объектов и отдали ему строку - result
            JSONObject json = new JSONObject(result);
            //дальше находим вход в наш json им является ключевое слово zapros
            JSONArray urls = json.getJSONArray("zapros");
                //читаем что в себе хранит параметр prodid
                prodid=(urls.getJSONObject(0).getString("prodid").toString());
                Log.d(TAG, "prodid:" + prodid);
                //читаем что в себе хранит параметр tovar
                tovar=(urls.getJSONObject(1).getString("tovar").toString());
                Log.d(TAG, "tovar:" + tovar);
                //читаем что в себе хранит параметр mx
                cfcv=(urls.getJSONObject(2).getString("cfc").toString());
                Log.d(TAG, "cfc:" + cfcv);
                //читаем что в себе хранит параметр cfcst
                cfcstr=(urls.getJSONObject(3).getString("cfcst").toString());
                Log.d(TAG, "cfcst:" + cfcstr);
                cfcarr = cfcstr.split(",");
                for (int i = 0; i < cfcarr.length; i++) {
                    Log.d(TAG, "cfcarr = " + cfcarr[i]);
                }
        }
        catch (JSONException e) {Log.e(TAG, "Error parsing data " + e.toString());}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
        if (id ==1) {
            adb.setTitle(R.string.cfc);
            adb.setSingleChoiceItems(cfcarr, 0, myClickListener);
            adb.setPositiveButton(R.string.ok, myClickListener);
        }
        if (id ==2) {
            adb.setTitle("Удаление товара");
            adb.setMessage("Удалить " + deltov + "?"); // сообщение
            adb.setPositiveButton(R.string.ok, delClickListener);
            adb.setNegativeButton(R.string.no, delClickListener);
        }
        if (id ==3) {
            adb.setTitle("Выбор места");
            adb.setMessage("Другой шкаф?"); // сообщение
            adb.setPositiveButton(R.string.ok, mxClickListener);
            adb.setNegativeButton(R.string.no, mxClickListener);
        }
        if (id ==4) {
            adb.setTitle("Выбор сотрудника");
            adb.setMessage("Новый сотрудник?"); // сообщение
            adb.setPositiveButton(R.string.ok, nmClickListener);
            adb.setNegativeButton(R.string.no, nmClickListener);
        }
        return adb.create();
    }

    // обработчик нажатия на пункт списка диалога или кнопку CFC
    DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            ListView lv = ((AlertDialog) dialog).getListView();
            if (which == Dialog.BUTTON_POSITIVE)
                Log.d(TAG, "pos = " + lv.getCheckedItemPosition());
            int dpcfc=(lv.getCheckedItemPosition());
            cfcv = cfcarr[dpcfc];
            CfcButton.setText(cfcv);
        }
    };
    // обработчик нажатия на пункт списка диалога или кнопку DEL
    DialogInterface.OnClickListener delClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (which == Dialog.BUTTON_POSITIVE) {
                Log.d(TAG, "pos = yes");
                Log.d(TAG, "Выполняем delete:" + server + "inverrlist.php");
                Log.d(TAG, "Выполняем delete:" + delpid + " Товар:" + deltov);
                new DelTovar().execute(server + "inverrlist.php");
            }
            if (which == Dialog.BUTTON_NEGATIVE) Log.d(TAG, "pos = no");
            removeDialog(2);
        }
    };
    // обработчик нажатия на пункт списка диалога или кнопку MX
    DialogInterface.OnClickListener mxClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (which == Dialog.BUTTON_POSITIVE) {
                Log.d(TAG, "pos = yes");
                Log.d(TAG, "MX:" + mxcode);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); //скроем клавиатуру
                Toast.makeText(getApplicationContext(), "MX:" + mxcode, Toast.LENGTH_LONG).show();
                new RequestMX().execute(server + "invfmx.php");
            }
            if (which == Dialog.BUTTON_NEGATIVE) Log.d(TAG, "pos = no");
            removeDialog(3);
        }
    };
    // обработчик нажатия на пункт списка диалога или кнопку NAME
    DialogInterface.OnClickListener nmClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (which == Dialog.BUTTON_POSITIVE) {
                Log.d(TAG, "pos = yes");
                Log.d(TAG, "Сотрудник:" + namecode);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); //скроем клавиатуру
                Toast.makeText(getApplicationContext(), "Сотрудник:" + namecode, Toast.LENGTH_LONG).show();
                new RequestName().execute(server + "invname.php");
            }
            if (which == Dialog.BUTTON_NEGATIVE) Log.d(TAG, "pos = no");
            removeDialog(4);
        }
    };

    public void save() {
        Log.d(TAG, "SAVE");
        Log.d(TAG, "firm = " + firmcode);
        Log.d(TAG, "mx = " + mxcode);
        Log.d(TAG, "seller = " + namecode);
        Log.d(TAG, "roles = " + roles);
        Log.d(TAG, "barcode = " + barcode);
        Log.d(TAG, "prodcode = " + prodid);
        Log.d(TAG, "cfc = " + cfcv);
        String tov = prodname.getText().toString();
        vqty = qty.getText().toString();
        if (vqty.equals("")) vqty = qty.getHint().toString();
        Log.d(TAG, "qty = " + vqty);
        //Проверим наличие всех значений и параметров
        if (tov.equals("null")||tov.equals("")||tov.equals("")||pref.equals("250")||firmcode.equals("")||mxcode.equals("")||namecode.equals("")||roles.equals("")||barcode.equals("")||prodid.equals("")||cfcv.equals("")||vqty.equals("")) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); //скроем клавиатуру
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
        }
        else {
            Log.d(TAG, "Выполняем Insert:" + server + "invsave.php");
            Toast.makeText(this, "Товар записан", Toast.LENGTH_SHORT).show();
            new SaveTovar().execute(server + "invsave.php");
        }
    }
    class SaveTovar extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                //создаем запрос на сервер
                DefaultHttpClient hc = new DefaultHttpClient();
                ResponseHandler<String> res = new BasicResponseHandler();
                //он у нас будет посылать post запрос
                HttpPost postMethod = new HttpPost(params[0]);
                //будем передавать параметры
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(8);
                //передаем параметры из наших текстбоксов
                nameValuePairs.add(new BasicNameValuePair("firm", firmcode));
                nameValuePairs.add(new BasicNameValuePair("mx", mxcode));
                nameValuePairs.add(new BasicNameValuePair("seller", namecode));
                nameValuePairs.add(new BasicNameValuePair("roles", roles));
                nameValuePairs.add(new BasicNameValuePair("barcode", barcode));
                nameValuePairs.add(new BasicNameValuePair("prodcode", prodid));
                nameValuePairs.add(new BasicNameValuePair("cfc", cfcv));
                nameValuePairs.add(new BasicNameValuePair("qty", vqty));
                //собераем их вместе и посылаем на сервер
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                //получаем ответ от сервера
                String response = hc.execute(postMethod, res);
                Log.d(TAG, "Запрос http" + response);
                JSONURLSAVE(response);
            } catch (Exception e) {
                System.out.println("Exp=" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dialog.dismiss();
            super.onPostExecute(result);
            ScanText.setHint(barcode);
            sbarcode = barcode;
            barcode = "";
            qty = (EditText) findViewById(R.id.qty);
            qty.setText("");
            qty.setHint("1");
            prodname = (TextView) findViewById(R.id.prodname);
            prodname.setText(rls + String.valueOf(Integer.parseInt(cfcv) * Integer.parseInt(vqty)) + tovar);
            CfcButton = (Button) findViewById(R.id.CfcButton);
            CfcButton.setText("Ед.");
            HashMap<String, Object> hm;
            hm = new HashMap<String, Object>();
            hm.put(mxPID, prodid);
            hm.put(mxTOV, tovar);
            hm.put(mxQTY, String.valueOf(Integer.parseInt(cfcv) * Integer.parseInt(vqty)));
            mxList.add(hm);
            //дальше добавляем полученные параметры в наш адаптер
            SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, mxList, R.layout.list,
                    new String[]{mxPID, mxTOV, mxQTY}, new int[]{R.id.text3, R.id.text1, R.id.text2});
            //выводим в листвбю
            listView = (ListView) findViewById(R.id.list);
            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.smoothScrollToPosition(mxList.size());
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Загружаюсь...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show();
            super.onPreExecute();
        }
    }
    public void JSONURLSAVE(String result) {
        try {
            //создали читателя json объектов и отдали ему строку - result
            JSONObject json = new JSONObject(result);
            //дальше находим вход в наш json им является ключевое слово zapros
            JSONArray urls = json.getJSONArray("resultsave");
            //читаем что в себе хранит параметр prodid
            rt=(urls.getJSONObject(0).getString("add").toString());
            Log.d(TAG, "rt:" + rt);
        }
        catch (JSONException e) {Log.e(TAG, "Error parsing data " + e.toString());}
    }
    class DelTovar extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                //создаем запрос на сервер
                DefaultHttpClient hc = new DefaultHttpClient();
                ResponseHandler<String> res = new BasicResponseHandler();
                //он у нас будет посылать post запрос
                HttpPost postMethod = new HttpPost(params[0]);
                //будем передавать параметры
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                //передаем параметры из наших текстбоксов
                nameValuePairs.add(new BasicNameValuePair("firm", firmcode));
                nameValuePairs.add(new BasicNameValuePair("mx", mxcode));
                nameValuePairs.add(new BasicNameValuePair("prodcode", delpid));
                Log.d(TAG, "firm" + firmcode);
                Log.d(TAG, "mx" + mxcode);
                Log.d(TAG, "prodcode" + delpid);
                //собераем их вместе и посылаем на сервер
                postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                //получаем ответ от сервера
                String response = hc.execute(postMethod, res);
                Log.d(TAG, "Запрос http" + response);
                JSONURLDEL(response);
            } catch (Exception e) {
                System.out.println("Exp=" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dialog.dismiss();
            super.onPostExecute(result);
            Log.d(TAG, "Запрос mx:" + server + "invfmx.php");
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN); //скроем клавиатуру
            Toast.makeText(getApplicationContext(), "Удалено строк: " + rt, Toast.LENGTH_LONG).show();
            new RequestMX().execute(server + "invfmx.php");
        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Загружаюсь...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show();
            super.onPreExecute();
        }
    }
    public void JSONURLDEL(String result) {
        try {
            //создали читателя json объектов и отдали ему строку - result
            JSONObject json = new JSONObject(result);
            //дальше находим вход в наш json им является ключевое слово zapros
            JSONArray urls = json.getJSONArray("resulterr");
            //читаем что в себе хранит параметр prodid
            rt=(urls.getJSONObject(0).getString("del").toString());
            Log.d(TAG, "rt:" + rt);
        }
        catch (JSONException e) {Log.e(TAG, "Error parsing data " + e.toString());}
    }
}

    /* Проверка контрольного числа EAN-13, смысла не т так как камера кч не возвращает а рассчитывает
    public void controlbarcode() {
        Log.d(TAG, "Проверка");
        bct = false;
        barcodeArray = barcode.toCharArray();
        bcdl = barcodeArray.length;
        if (bcdl == 13) {
            String cbc = barcode.substring(barcodeArray.length - 1, barcodeArray.length);
            int chetVal = 0, nechetVal = 0;
            String codeToParse = barcode;
            for (int index = 0; index < 6; index++) {
                chetVal += Integer.valueOf(codeToParse.substring(index * 2 + 1, index * 2 + 2)).intValue();
                nechetVal += Integer.valueOf(codeToParse.substring(index * 2, index * 2 + 1)).intValue();
            }
            chetVal *= 3;
            int controlNumber = 10 - (chetVal + nechetVal) % 10;
            if (controlNumber == 10) controlNumber = 0;
            codeToParse = String.valueOf(controlNumber);
            if (cbc.equals(codeToParse)) {
                bct = true;
                Log.d(TAG, "Проверка выполнена" + cbc + codeToParse);
                select();
            } else Log.d(TAG, "Проверка не выполнена" + cbc + codeToParse);
        }
    }

@Override
    public boolean onTouchEvent(final MotionEvent event){

        Camera.Parameters cameraParameters = camera.getParameters();
        if (event.getAction() == MotionEvent.ACTION_UP){
            focusAreas.clear();
            meteringAreas.clear();
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
            Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);
            focusAreas.add(new Camera.Area(focusRect, 800));
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            cameraParameters.setFocusAreas(focusAreas);
            cameraParameters.setMeteringAreas(meteringAreas);
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            try{
                camera.setParameters(cameraParameters);

            } catch(Exception e){
                Log.e("Focus problem", e.toString());
                return false;
            }

            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    camera.cancelAutoFocus();
                    Camera.Parameters  params = camera.getParameters();
                    if(params.getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE){
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        camera.setParameters(params);
                    }
                }
            });

            focusSound = new MediaPlayer();
            showSquareFocus();
            try {
                AssetFileDescriptor descriptor = this.getApplicationContext().getAssets()
                        .openFd("focus.wav");
                focusSound.setDataSource(descriptor.getFileDescriptor(),
                        descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close();
                focusSound.prepare();
                focusSound.setLooping(false);
                focusSound.start();
                focusSound.setVolume(10,10);
                focusSound.setOnCompletionListener(new OnCompletionListener(){
                    public void onCompletion(MediaPlayer mp){
                        mp.release();
                      }
                    });
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    private Rect calculateTapArea(float x, float y, float coefficient) {
            int areaSize = Float.valueOf(FOCUS_AREA_SIZE * coefficient).intValue();
            int left = clamp((int) x - areaSize / 2, 0, width - areaSize);
            int top = clamp((int) y - areaSize / 2, 0, height - areaSize);
            RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
            matrix.mapRect(rectF);
            return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
        }

        private int clamp(int x, int min, int max) {
            if (x > max) {
                return max;
            }
            if (x < min) {
                return min;
            }
            return x;
        }
*/