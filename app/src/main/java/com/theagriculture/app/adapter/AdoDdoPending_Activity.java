package com.theagriculture.app.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.theagriculture.app.Admin.AdoDdoActivity.AdoDdoListAdapter;
import com.theagriculture.app.Globals;
import com.theagriculture.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdoDdoPending_Activity extends AppCompatActivity {

    private String mDdoId;
    private ArrayList<String> locationNames;
    private ArrayList<String> locationAddresses;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayoutManager layoutManager;
    private AdoDdoListAdapter adapter;
    private int NEXT_LOCATION_COUNT = 1;
    private String nextAssignedUrl;
    private String nextUnAssignedUrl;
    private String nextPendingUrl;  //for ADO
    private boolean isDdo;
    private String token;
    private String TAG="adoddopending";
    private String mUrlUnAssigned;
    private RequestQueue queue;
    private boolean flag= false;
    private ArrayList<String> mAdoNames;
    private boolean isNextBusy = false;
    private SwipeRefreshLayout swipeRefreshLayout;
    ProgressBar spinner;
    boolean isVisible=false;
    boolean doubleBackToExitPressedOnce = false;
    String mUrl;


    public AdoDdoPending_Activity() {
        // Required empty public constructor
    }


    public AdoDdoPending_Activity(String mDdoId, boolean isDdo) {
        this.mDdoId = mDdoId;
        this.isDdo = isDdo;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_ddo_pending);
        Log.d(TAG,"in onCreate: ");

        spinner = (ProgressBar)findViewById(R.id.Ddo_pending_loading_json);
        spinner.setVisibility(View.VISIBLE);
        progressBar = findViewById(R.id.Ddo_pending_loading);
        recyclerView = findViewById(R.id.Ddo_pending_recyclerview);
        layoutManager = new LinearLayoutManager(this);
        swipeRefreshLayout = findViewById(R.id.refreshpull7);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                getFragmentManager().beginTransaction().detach(AdoDdoPending_Activity.this).attach(AdoDdoPending.this).commit();
            }
        });
        recyclerView.setLayoutManager(layoutManager);

        locationAddresses = new ArrayList<>();
        mAdoNames = new ArrayList<>();
        if (isDdo)
            adapter = new AdoDdoListAdapter(this, locationNames, locationAddresses, mAdoNames, true);
        else
            adapter = new AdoDdoListAdapter(this, locationNames, locationAddresses, mAdoNames, false);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration divider = new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation());
        recyclerView.addItemDecoration(divider);
        SharedPreferences prefs = this.getSharedPreferences("tokenFile", Context.MODE_PRIVATE);
        token = prefs.getString("token", "");

        String role;
        if (isDdo) {
            role = "dda";
            String mUrlAssigned = Globals.admin + role + "/" + mDdoId + "/assigned";                        //"http://18.224.202.135/api/admin/" + role + "/" + mDdoId + "/assigned";
            mUrlUnAssigned = Globals.admin + role + "/" + mDdoId + "/unassigned" ;                          //"http://18.224.202.135/api/admin/" + role + "/" + mDdoId + "/unassigned";
            Log.d("url", "onCreateView: pending" + mUrlAssigned);
            Log.d("url", "onCreateView: pending" + mUrlUnAssigned);
            getData(mUrlAssigned);

        } else {
            role = "ado";
            String mUrlPending = Globals.admin + role + "/" + mDdoId + "/pending";                           //"http://18.224.202.135/api/admin/" + role + "/" + mDdoId + "/pending";
            Log.d("url", "onCreateView: pending" + mUrlPending);
            getData(mUrlPending);
        }
    }

    private void getData(final String url) {

        queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject rootObject = new JSONObject(String.valueOf(response));
                            String nextUrl = rootObject.getString("next");
                            if (isDdo)
                                nextAssignedUrl = nextUrl;
                            else
                                nextPendingUrl = nextUrl;
                            JSONArray resultsArray = rootObject.getJSONArray("results");
                            Log.d(TAG, "onResponse: "+resultsArray.length());
                            if (!isDdo && resultsArray.length() == 0) {
                                // adapter.mshowshimmer = false;
                                adapter.notifyDataSetChanged();
                                AdoDdoPending_Activity.this.getLayoutInflater().inflate(R.layout.fragment_nothing_toshow, null);
//                                Log.d(TAG, "onResponse: see here.... " + view);
//                                nothing_toshow_fragment no_data = new nothing_toshow_fragment();
//                                AppCompatActivity activity = (AppCompatActivity)getContext();
//                                activity.getSupportFragmentManager().beginTransaction().replace(R.id.change_nodata_pending, no_data).commit();
                                //view.setBackground(getActivity().getResources().getDrawable(R.drawable.ic_group_217));
                            }
                            for (int i = 0; i < resultsArray.length(); i++) {
                                JSONObject singleObject = resultsArray.getJSONObject(i);
                                if (isDdo) {
                                    try {
                                        JSONObject adoObject = singleObject.getJSONObject("ado");
                                        String adoName = adoObject.getString("name");
                                        mAdoNames.add(adoName);
                                    } catch (JSONException e) {
                                        mAdoNames.add("Not Assigned");
                                    }
                                }
                                String locName = singleObject.getString("village_name");
                                String locAdd = singleObject.getString("block_name") +
                                        ", " + singleObject.getString("district");
                                locationNames.add(locName);
                                locationAddresses.add(locAdd);
                            }
                            if (isDdo)
                                getdata1(mUrlUnAssigned);
                            else {
                                //adapter.mshowshimmer = false;
                                adapter.notifyDataSetChanged();
                            }
                            spinner.setVisibility(View.GONE);
                        } catch (JSONException e) {
                            spinner.setVisibility(View.GONE);
                            Toast.makeText(AdoDdoPending_Activity.this, "An exception occured", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        spinner.setVisibility(View.GONE);
                        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                            //This indicates that the reuest has either time out or there is no connection
                            //if(isVisible)
                            //Toast.makeText(getActivity(), "This error is case1", Toast.LENGTH_LONG).show();
                            final BottomSheetDialog mBottomDialogNotificationAction = new BottomSheetDialog(AdoDdoPending_Activity.this);
                            View sheetView = AdoDdoPending_Activity.this.getLayoutInflater().inflate(R.layout.no_internet, null);
                            mBottomDialogNotificationAction.setContentView(sheetView);
                            mBottomDialogNotificationAction.setCanceledOnTouchOutside(false);
                            mBottomDialogNotificationAction.setCancelable(false);
                            mBottomDialogNotificationAction.show();

                            // Remove default white color background

                            FrameLayout bottomSheet = (FrameLayout) mBottomDialogNotificationAction.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            bottomSheet.setBackground(null);


                            TextView close = sheetView.findViewById(R.id.close);
                            Button retry = sheetView.findViewById(R.id.retry);

                            retry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mBottomDialogNotificationAction.dismiss();
                                    spinner.setVisibility(View.VISIBLE);
                                    getData(url);
                                }
                            });

                            close.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!doubleBackToExitPressedOnce) {
                                        doubleBackToExitPressedOnce = true;
                                        Toast toast = Toast.makeText(AdoDdoPending_Activity.this, "Tap on Close App again to exit app", Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();


                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                doubleBackToExitPressedOnce = false;
                                            }
                                        }, 3600);
                                    } else {
                                        mBottomDialogNotificationAction.dismiss();
                                        Intent a = new Intent(Intent.ACTION_MAIN);//will exit app
                                        a.addCategory(Intent.CATEGORY_HOME);
                                        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(a);
                                    }
                                }

                            });


                        } else if (error instanceof AuthFailureError) {
                            // Error indicating that there was an Authentication Failure while performing the request
                            Toast.makeText(AdoDdoPending_Activity.this, "This error is case2", Toast.LENGTH_LONG).show();
                        } else if (error instanceof ServerError) {
                            //Indicates that the server responded with a error response
                            Toast.makeText(AdoDdoPending_Activity.this, "This error is server error", Toast.LENGTH_LONG).show();
                        } else if (error instanceof NetworkError) {
                            //Indicates that there was network error while performing the request
                            Toast.makeText(AdoDdoPending_Activity.this, "This error is case4", Toast.LENGTH_LONG).show();
                        } else if (error instanceof ParseError) {
                            // Indicates that the server response could not be parsed
                            Toast.makeText(AdoDdoPending_Activity.this, "This error is case5", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdoDdoPending_Activity.this, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                        }
                        //Log.d("pending", "onErrorResponse: " + error);
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                map.put("Authorization", "Token " + token);
                return map;
            }
        };
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int totalCount, pastItemCount, visibleItemCount;
                if (dy > 0) {
                    totalCount = layoutManager.getItemCount();
                    pastItemCount = layoutManager.findFirstVisibleItemPosition();
                    visibleItemCount = layoutManager.getChildCount();
                    if ((pastItemCount + visibleItemCount) >= totalCount) {
                        if (!isNextBusy)
                            loadNextLocations();
                    }
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        queue.add(jsonObjectRequest);

    }

    public void getdata1(final String url){
        JsonObjectRequest jsonObjectRequest1 = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject rootObject = new JSONObject(String.valueOf(response));
                            String nextUrl = rootObject.getString("next");
                            nextUnAssignedUrl = nextUrl;
                            JSONArray resultsArray = rootObject.getJSONArray("results");
                            if (resultsArray.length() == 0 && mAdoNames.isEmpty()) {
                                //adapter.mshowshimmer = false;
                                adapter.notifyDataSetChanged();

                                //todo image here
                                AdoDdoPending_Activity.this.getLayoutInflater().inflate(R.layout.fragment_nothing_toshow, null);
//                                nothing_toshow_fragment no_data = new nothing_toshow_fragment();
//                                AppCompatActivity activity = (AppCompatActivity)getContext();
//                                activity.getSupportFragmentManager().beginTransaction().replace(R.id.change_nodata_pending, no_data).commit();
                                //view.setBackground(getActivity().getResources().getDrawable(R.drawable.svg_nothing_toshow_1));
                                //view.getView().setBackground(getActivity().getResources().getDrawable(R.drawable.no_entry_background));
                            }
                            Log.d(TAG, "onResponse: "+resultsArray.length());

                            for (int i = 0; i < resultsArray.length(); i++) {
                                JSONObject singleObject = resultsArray.getJSONObject(i);
                                if (isDdo) {
                                    try {
                                        JSONObject adoObject = singleObject.getJSONObject("ado");
                                        String adoName = adoObject.getString("name");
                                        mAdoNames.add(adoName);
                                    } catch (JSONException e) {
                                        mAdoNames.add("Not Assigned");
                                    }
                                }
                                String locName = singleObject.getString("village_name");
                                String locAdd = singleObject.getString("block_name") +
                                        ", " + singleObject.getString("district");
                                locationNames.add(locName);
                                locationAddresses.add(locAdd);
                            }
                            Log.d(TAG, "onResponse: ADO NAMES: " + mAdoNames);
                            //adapter.mshowshimmer = false;
                            adapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("pending", "onErrorResponse: " + error);
                        if(error instanceof TimeoutError || error instanceof NoConnectionError){
                            final BottomSheetDialog mBottomDialogNotificationAction = new BottomSheetDialog(AdoDdoPending_Activity.this);
                            View sheetView = AdoDdoPending_Activity.this.getLayoutInflater().inflate(R.layout.no_internet, null);
                            mBottomDialogNotificationAction.setContentView(sheetView);
                            mBottomDialogNotificationAction.setCanceledOnTouchOutside(false);
                            mBottomDialogNotificationAction.setCancelable(false);
                            mBottomDialogNotificationAction.show();

                            // Remove default white color background

                            FrameLayout bottomSheet = (FrameLayout) mBottomDialogNotificationAction.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            bottomSheet.setBackground(null);


                            TextView close = sheetView.findViewById(R.id.close);
                            Button retry = sheetView.findViewById(R.id.retry);

                            retry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mBottomDialogNotificationAction.dismiss();
                                    //progressBar.setVisibility(View.VISIBLE);
                                    //spinner.setVisibility(View.VISIBLE);
                                    getdata1(url);
                                }
                            });

                            close.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!doubleBackToExitPressedOnce) {
                                        doubleBackToExitPressedOnce = true;
                                        Toast toast = Toast.makeText(AdoDdoPending_Activity.this, "Tap on Close App again to exit app", Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();


                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                doubleBackToExitPressedOnce = false;
                                            }
                                        }, 3600);
                                    } else {
                                        mBottomDialogNotificationAction.dismiss();
                                        Intent a = new Intent(Intent.ACTION_MAIN);//will exit app
                                        a.addCategory(Intent.CATEGORY_HOME);
                                        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(a);
                                    }
                                }

                            });
                        }
                        else if (error instanceof AuthFailureError) {
                            // Error indicating that there was an Authentication Failure while performing the request
                            Toast.makeText(AdoDdoPending_Activity.this, "This error is case2", Toast.LENGTH_LONG).show();
                        } else if (error instanceof ServerError) {
                            //Indicates that the server responded with a error response
                            Toast.makeText(AdoDdoPending_Activity.this, "This is a server error", Toast.LENGTH_LONG).show();
                        } else if (error instanceof NetworkError) {
                            //Indicates that there was network error while performing the request
                            Toast.makeText(AdoDdoPending_Activity.this, "This error is case4", Toast.LENGTH_LONG).show();
                        } else if (error instanceof ParseError) {
                            // Indicates that the server response could not be parsed
                            Toast.makeText(AdoDdoPending_Activity.this, "This error is case5", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdoDdoPending_Activity.this, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                map.put("Authorization", "Token " + token);
                return map;
            }
        };
        queue.add(jsonObjectRequest1);

    }

    private void loadNextLocations() {
        if (isDdo) {
            switch (NEXT_LOCATION_COUNT) {
                case 1:
                    if (!nextAssignedUrl.equals("null"))
                        makeRequest(nextAssignedUrl, true);
                    NEXT_LOCATION_COUNT = 2;
                    break;
                case 2:
                    if (!nextUnAssignedUrl.equals("null"))
                        makeRequest(nextUnAssignedUrl, false);
                    NEXT_LOCATION_COUNT = 1;
                    break;
            }
        } else {
            makeRequest(nextPendingUrl, true);
        }
    }

    private void makeRequest(String url, final boolean isAssigned) {
        RequestQueue queue = Volley.newRequestQueue(AdoDdoPending_Activity.this);
        isNextBusy = true;
        progressBar.setVisibility(View.VISIBLE);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject rootObject = new JSONObject(String.valueOf(response));
                            JSONArray resultsArray = rootObject.getJSONArray("results");
                            String nextUrl = rootObject.getString("next");
                            if (isDdo) {
                                if (isAssigned)
                                    nextAssignedUrl = nextUrl;
                                else
                                    nextUnAssignedUrl = nextUrl;
                            } else
                                nextPendingUrl = nextUrl;
                            for (int i = 0; i < resultsArray.length(); i++) {
                                JSONObject singleObject = resultsArray.getJSONObject(i);
                                if (isDdo) {
                                    try {
                                        JSONObject adoObject = singleObject.getJSONObject("ado");
                                        String adoName = adoObject.getString("name");
                                        mAdoNames.add(adoName);
                                    } catch (JSONException e) {
                                        mAdoNames.add("Not Assigned");
                                    }
                                }
                                String locName = singleObject.getString("village_name");
                                String locAdd = singleObject.getString("block_name") +
                                        ", " + singleObject.getString("state");
                                locationNames.add(locName);
                                locationAddresses.add(locAdd);
                            }
                            adapter.notifyDataSetChanged();
                            isNextBusy = false;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(error instanceof TimeoutError || error instanceof NoConnectionError){
                            final BottomSheetDialog mBottomDialogNotificationAction = new BottomSheetDialog(AdoDdoPending_Activity.this);
                            View sheetView = AdoDdoPending_Activity.this.getLayoutInflater().inflate(R.layout.no_internet, null);
                            mBottomDialogNotificationAction.setContentView(sheetView);
                            mBottomDialogNotificationAction.setCanceledOnTouchOutside(false);
                            mBottomDialogNotificationAction.setCancelable(false);
                            mBottomDialogNotificationAction.show();

                            // Remove default white color background

                            FrameLayout bottomSheet = (FrameLayout) mBottomDialogNotificationAction.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                            bottomSheet.setBackground(null);


                            TextView close = sheetView.findViewById(R.id.close);
                            Button retry = sheetView.findViewById(R.id.retry);

                            retry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mBottomDialogNotificationAction.dismiss();
                                    //progressBar.setVisibility(View.VISIBLE);
                                    //spinner.setVisibility(View.VISIBLE);
                                    makeRequest(nextAssignedUrl,isAssigned);
                                }
                            });

                            close.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!doubleBackToExitPressedOnce) {
                                        doubleBackToExitPressedOnce = true;
                                        Toast toast = Toast.makeText(AdoDdoPending_Activity.this, "Tap on Close App again to exit app", Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();


                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                doubleBackToExitPressedOnce = false;
                                            }
                                        }, 3600);
                                    } else {
                                        mBottomDialogNotificationAction.dismiss();
                                        Intent a = new Intent(Intent.ACTION_MAIN);//will exit app
                                        a.addCategory(Intent.CATEGORY_HOME);
                                        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(a);
                                    }
                                }

                            });

                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                map.put("Authorization", "Token " + token);
                return map;
            }
        };
        queue.add(jsonObjectRequest);
        requestFinished(queue);

    }

    private void requestFinished(RequestQueue queue) {

        queue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

            @Override
            public void onRequestFinished(Request<Object> request) {
                progressBar.setVisibility(View.GONE);
            }
        });

    }

}
