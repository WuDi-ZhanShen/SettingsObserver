package allsettings.observer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends Activity {
    ListView lv;
    private ObService obService;
    boolean b = false;

    ServiceConnection conn;
    private adapter ap;
    static String newdata = null;

    static class adapter extends BaseAdapter {

        private final List<String[]> history;
        private final Context context;

        adapter(Context context) {
            this.context = context;
            history = new ArrayList<>();
        }

        public void add(String[] data) {
            newdata = data[0] + " " + data[1] + " " + data[2];
            data[3] = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
            history.add(0,data);
            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            return history.size();
        }

        @Override
        public Object getItem(int i) {
            return history.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }


        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(context);
            ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.item, null);
                holder = new ViewHolder();
                holder.t1 = view.findViewById(R.id.t1);
                holder.t2 = view.findViewById(R.id.t2);
                holder.t3 = view.findViewById(R.id.t3);
                holder.t4 = view.findViewById(R.id.t4);
                holder.b = view.findViewById(R.id.b);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            String[] change = history.get(i);
            holder.t1.setText(String.format("?????????%s", change[3]));
            holder.t2.setText(String.format("?????????%s", change[0]));
            holder.t3.setText(String.format("?????????%s", change[1]));
            holder.t4.setText(String.format("??????%s", change[2]));
            holder.b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String inline = change[0] + " " + change[1] + " " + change[2];
                    ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", inline));
                    Toast.makeText(context, "????????????????????????\n" + inline, Toast.LENGTH_SHORT).show();
                }
            });
            return view;
        }

        static class ViewHolder {
            TextView t1, t2, t3, t4;
            Button b;
        }
    }

    private BroadcastReceiver mBroadcastReceiver,mBroadcastReceiver1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout);
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ?????????????????? navigationBarColor ???????????????
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (newdata != null) {
                    Toast.makeText(context, "????????????????????????\n" + newdata, Toast.LENGTH_SHORT).show();
                    ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", newdata));
                }
            }
        };
        mBroadcastReceiver1 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        registerReceiver(mBroadcastReceiver, new IntentFilter("intent.COPY"));
        registerReceiver(mBroadcastReceiver1, new IntentFilter("intent.EXIT"));

        lv = findViewById(R.id.l);
        ap = new adapter(this);
        lv.setAdapter(ap);

        conn = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {


                //????????????MsgService??????
                obService = ((ObService.MsgBinder) service).getService();

                //????????????????????????????????????????????????
                obService.setChangeListener(new ObService.ChangeListener() {

                    @Override
                    public void onChange(String[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ap.add(data);
                            }
                        });
                    }


                });

            }
        };
        Switch s = findViewById(R.id.s);
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    if (!b) {
                        b = true;
                        bindService(new Intent(MainActivity.this, ObService.class), conn, Context.BIND_AUTO_CREATE);
                    }

                } else {
                    if (b) {
                        b = false;
                        unbindService(conn);
                    }

                }

            }
        });
        if (!b) {
            b = true;
            bindService(new Intent(this, ObService.class), conn, Context.BIND_AUTO_CREATE);
        }


    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        unregisterReceiver(mBroadcastReceiver1);
        if (b)
            unbindService(conn);
        super.onDestroy();
    }
}