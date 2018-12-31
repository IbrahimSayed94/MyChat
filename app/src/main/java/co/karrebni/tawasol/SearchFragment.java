package co.karrebni.tawasol;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.karrebni.tawasol.adapter.SearchListAdapter;
import co.karrebni.tawasol.app.App;
import co.karrebni.tawasol.constants.Constants;
import co.karrebni.tawasol.dialogs.PeopleNearbySettingsDialog;
import co.karrebni.tawasol.dialogs.SearchSettingsDialog;
import co.karrebni.tawasol.model.User;
import co.karrebni.tawasol.util.CustomRequest;
import co.karrebni.tawasol.view.PeopleNearbySettingsDialog1;

public class SearchFragment extends Fragment implements Constants, SwipeRefreshLayout.OnRefreshListener {

    private static final String STATE_LIST = "State Adapter Data";

    SearchView searchView = null;

    ListView mListView;
    TextView mMessage, mHeaderText, mHeaderSettings;
    ImageView mSplash;

    LinearLayout mHeaderContainer;

    SwipeRefreshLayout mItemsContainer;

    private ArrayList<User> itemsList;
    private SearchListAdapter itemsAdapter;

    public String queryText, currentQuery, oldQuery;

    public int itemCount;
    private int userId = 0;

    private int search_gender = -1, search_online = -1, preload_gender = -1;

    private int itemId = 0;
    private int arrayLength = 0;
    private Boolean loadingMore = false;
    private Boolean viewMore = false;
    private Boolean restore = false;
    private Boolean preload = true;
    private  int flag = 0 ;

    Dialog filterDialog , genderDialog;
    Button bt_ok_filter , bt_ok_gender;
    RadioGroup radioGroup_filter ;
    String filterType = "" , genderType="all";
    CheckBox ch_male , ch_female ;
    int male = 0 , female = 0 ;

    private int distance = 50;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        initFilterDialog(rootView);
        initGenderDialog();

        if (savedInstanceState != null) {

            itemsList = savedInstanceState.getParcelableArrayList(STATE_LIST);
            itemsAdapter = new SearchListAdapter(getActivity(), itemsList);

            currentQuery = queryText = savedInstanceState.getString("queryText");

            restore = savedInstanceState.getBoolean("restore");
            preload = savedInstanceState.getBoolean("preload");
            itemId = savedInstanceState.getInt("itemId");
            userId = savedInstanceState.getInt("userId");
            itemCount = savedInstanceState.getInt("itemCount");
            search_gender = savedInstanceState.getInt("search_gender");
            preload_gender = savedInstanceState.getInt("preload_gender");

        } else {

            itemsList = new ArrayList<User>();
            itemsAdapter = new SearchListAdapter(getActivity(), itemsList);

            currentQuery = queryText = "";

            restore = false;
            preload = true;
            itemId = 0;
            userId = 0;
            itemCount = 0;
            search_gender = -1;
            preload_gender = -1;
        }

        mHeaderContainer = (LinearLayout) rootView.findViewById(R.id.container_header);
        mHeaderText = (TextView) rootView.findViewById(R.id.headerText);
        mHeaderSettings = (TextView) rootView.findViewById(R.id.headerSettings);

        mItemsContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.container_items);
        mItemsContainer.setOnRefreshListener(this);

        mMessage = (TextView) rootView.findViewById(R.id.message);
        mSplash = (ImageView) rootView.findViewById(R.id.splash);

        mListView = (ListView) rootView.findViewById(R.id.listView);

        mListView.setAdapter(itemsAdapter);

        if (itemsAdapter.getCount() == 0) {

            showMessage(getText(R.string.label_empty_list).toString());

        } else {

            hideMessage();
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                User u = (User) adapterView.getItemAtPosition(position);

                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra("profileId", u.getId());
                startActivity(intent);
            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                int lastInScreen = firstVisibleItem + visibleItemCount;

                if ((lastInScreen == totalItemCount) && !(loadingMore) && (viewMore) && !(mItemsContainer.isRefreshing())) {

                    if (preload) {

                        loadingMore = true;

                        preload();

                    } else {

                        currentQuery = getCurrentQuery();

                        flag = 2;

                        if(flag != 2) {
                            if (currentQuery.equals(oldQuery)) {

                                loadingMore = true;
                                Toast.makeText(getContext(), "" + currentQuery, Toast.LENGTH_SHORT).show();

                                search();
                            }
                        }
                    }
                }
            }
        });

        if (queryText.length() == 0) {

            if (mListView.getAdapter().getCount() == 0) {

                showMessage(getString(R.string.label_search_start_screen_msg));
                mHeaderText.setVisibility(View.GONE);

            } else {

                if (preload) {

                    mHeaderText.setVisibility(View.GONE);

                } else {

                    mHeaderText.setVisibility(View.VISIBLE);
                    mHeaderText.setText(getText(R.string.label_search_results) + " " + Integer.toString(itemCount));
                }

                hideMessage();
            }

        } else {

            if (mListView.getAdapter().getCount() == 0) {

                showMessage(getString(R.string.label_search_results_error));
                mHeaderText.setVisibility(View.GONE);

            } else {

                mHeaderText.setVisibility(View.VISIBLE);
                mHeaderText.setText(getText(R.string.label_search_results) + " " + Integer.toString(itemCount));

                hideMessage();
            }
        }

        mHeaderSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //filterWithGender();
                filterDialog.show();
            }
        });

        if (!restore) {

            if (preload) {

                preload();
            }
        }

        // Inflate the layout for this fragment
        return rootView;
    }


   /* private void filterWithGender()
    {
        *//** Getting the fragment manager *//*
        android.app.FragmentManager fm = getActivity().getFragmentManager();

        *//** Instantiating the DialogFragment class *//*
        SearchSettingsDialog alert = new SearchSettingsDialog();

        *//** Creating a bundle object to store the selected item's index *//*
        Bundle b  = new Bundle();

        *//** Storing the selected item's index in the bundle object *//*
        b.putInt("searchGender", search_gender);
        b.putInt("searchOnline", search_online);


        *//** Setting the bundle object to the dialog fragment object *//*
        alert.setArguments(b);

        *//** Creating the dialog fragment object, which will in turn open the alert dialog window *//*

        alert.show(fm, "alert_dialog_search_settings");

        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    } // function of filterWithGender*/

    public void onCloseSettingsDialog(int searchGender, int searchOnline) {

        search_gender = searchGender;
        search_gender = searchOnline;

        String q = getCurrentQuery();
        Toast.makeText(getContext(),""+search_gender+search_gender,Toast.LENGTH_SHORT).show();

        if (preload) {

            itemId = 0;

            preload();

        } else {

            if (q.length() > 0) {

                searchStart();
            }
        }
    }

    @Override
    public void onRefresh() {

        currentQuery = queryText;

        currentQuery = currentQuery.trim();

        if (App.getInstance().isConnected() && currentQuery.length() != 0) {

            userId = 0;
            search();

        } else {

            mItemsContainer.setRefreshing(false);
        }
    }

    public String getCurrentQuery() {

        String searchText = searchView.getQuery().toString();
        searchText = searchText.trim();

        return searchText;
    }

    public void searchStart() {

        preload = false;

        currentQuery = getCurrentQuery();

        if (App.getInstance().isConnected()) {

            userId = 0;
            search();

        } else {

            Toast.makeText(getActivity(), getText(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putString("queryText", queryText);
        outState.putBoolean("restore", true);
        outState.putBoolean("preload", preload);
        outState.putInt("itemId", itemId);
        outState.putInt("userId", userId);
        outState.putInt("itemCount", itemCount);
        outState.putInt("search_gender", search_gender);
        outState.putInt("preload_gender", preload_gender);
        outState.putParcelableArrayList(STATE_LIST, itemsList);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

//        MenuInflater menuInflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.options_menu_main_search);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        if (searchItem != null) {

            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        }

        if (searchView != null) {

            searchView.setQuery(queryText, false);

            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setIconifiedByDefault(false);
            searchView.setIconified(false);

            SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            searchAutoComplete.setHint(getText(R.string.placeholder_search));
            searchAutoComplete.setHintTextColor(getResources().getColor(R.color.white));

            searchView.clearFocus();

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {

                    queryText = newText;

                    return false;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {

                    queryText = query;
                    searchStart();

                    return false;
                }
            });
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    public void search() {

        mItemsContainer.setRefreshing(true);

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_APP_SEARCH, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            if (!loadingMore) {

                                itemsList.clear();
                            }

                            arrayLength = 0;

                            if (!response.getBoolean("error")) {

                                itemCount = response.getInt("itemCount");
                                oldQuery = response.getString("query");
                                userId = response.getInt("itemId");

                                if (response.has("items")) {

                                    JSONArray usersArray = response.getJSONArray("items");

                                    arrayLength = usersArray.length();

                                    if (arrayLength > 0) {

                                        for (int i = 0; i < usersArray.length(); i++) {

                                            JSONObject profileObj = (JSONObject) usersArray.get(i);

                                            User u = new User(profileObj);

                                            itemsList.add(u);
                                        }
                                    }
                                }
                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        } finally {

                            loadingComplete();

                            Log.e("response", response.toString());

//                            Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                loadingComplete();
                Toast.makeText(getActivity(), getString(R.string.error_data_loading), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("query", currentQuery);
                params.put("userId", Integer.toString(userId));
                params.put("gender", Integer.toString(search_gender));
                params.put("online", Integer.toString(search_online));
                params.put("distance", Integer.toString(distance));

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }

    public void preload() {

        if (preload) {

            mItemsContainer.setRefreshing(true);

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_APP_SEARCH_PRELOAD, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {

                                if (!loadingMore) {

                                    itemsList.clear();
                                }

                                arrayLength = 0;

                                if (!response.getBoolean("error")) {

                                    itemId = response.getInt("itemId");

                                    if (response.has("items")) {

                                        JSONArray usersArray = response.getJSONArray("items");

                                        arrayLength = usersArray.length();

                                        if (arrayLength > 0) {

                                            for (int i = 0; i < usersArray.length(); i++) {

                                                JSONObject profileObj = (JSONObject) usersArray.get(i);

                                                User u = new User(profileObj);

                                                itemsList.add(u);
                                            }
                                        }
                                    }
                                }

                            } catch (JSONException e) {

                                e.printStackTrace();

                            } finally {

                                loadingComplete();

//                            Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    loadingComplete();
                    Toast.makeText(getActivity(), getString(R.string.error_data_loading), Toast.LENGTH_LONG).show();
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());
                    params.put("itemId", Integer.toString(itemId));
                    params.put("gender", Integer.toString(search_gender));
                    params.put("online", Integer.toString(search_online));

                    return params;
                }
            };

            App.getInstance().addToRequestQueue(jsonReq);
        }
    }

    public void loadingComplete() {

        if (arrayLength == LIST_ITEMS) {

            viewMore = true;

        } else {

            viewMore = false;
        }

        itemsAdapter.notifyDataSetChanged();

        loadingMore = false;

        mItemsContainer.setRefreshing(false);

        if (mListView.getAdapter().getCount() == 0) {

            showMessage(getString(R.string.label_search_results_error));
            mHeaderText.setVisibility(View.GONE);

        } else {

            hideMessage();

            if (preload) {

                mHeaderText.setVisibility(View.GONE);

            } else {

                mHeaderText.setVisibility(View.VISIBLE);

                mHeaderText.setText(getText(R.string.label_search_results) + " " + Integer.toString(itemCount));
            }
        }
    }

    public void showMessage(String message) {

        mMessage.setText(message);
        mMessage.setVisibility(View.VISIBLE);

        mSplash.setVisibility(View.VISIBLE);
    }

    public void hideMessage() {

        mMessage.setVisibility(View.GONE);

        mSplash.setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void initFilterDialog(View view)
    {
        filterDialog = new Dialog(getContext());
        filterDialog.setContentView(R.layout.filter_dialog);

        bt_ok_filter = filterDialog.findViewById(R.id.bt_ok_filter);
        radioGroup_filter = filterDialog.findViewById(R.id.group_filter);

        radioGroup_filter.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                switch (i)
                {
                    case R.id.radio_gender : filterType = "gender";
                        break;
                    case R.id.radio_distance : filterType = "distance";
                        break;
                    default: filterType = "gender";
                        break;
                }
            }
        });

        bt_ok_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(filterType.equals(""))
                    Toast.makeText(getContext(),getString(R.string.pleaseChooseFilterType),Toast.LENGTH_LONG).show();

                else {
                    Log.e("QP","filter : "+filterType);
                    filterDialog.dismiss();

                    if(filterType.equals("distance"))filterWithDistance();
                    else if(filterType.equals("gender")) filterWithGender();
                }
            }
        });
    } // function of initFilterDialog

    private  void filterWithDistance()
    {
        /** Getting the fragment manager */
        android.app.FragmentManager fm = getActivity().getFragmentManager();

        /** Instantiating the DialogFragment class */
        PeopleNearbySettingsDialog1 alert = new PeopleNearbySettingsDialog1();

        /** Creating a bundle object to store the selected item's index */
        Bundle b  = new Bundle();

        /** Storing the selected item's index in the bundle object */
        b.putInt("distance", distance);

        /** Setting the bundle object to the dialog fragment object */
        alert.setArguments(b);

        /** Creating the dialog fragment object, which will in turn open the alert dialog window */

        alert.show(fm, "alert_dialog_nearby_settings");
    } // function of filterWithDistance

    private void initGenderDialog()
    {
        genderDialog = new Dialog(getContext());
        genderDialog.setContentView(R.layout.filter_gender_dialog);

        ch_male = genderDialog.findViewById(R.id.ch_male);
        ch_female = genderDialog.findViewById(R.id.ch__female);

        bt_ok_gender = genderDialog.findViewById(R.id.bt_ok_gender);

        ch_male.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b == true)
                    male = 1 ;
                else
                    male = 0 ;
            }
        });

        ch_female.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b == true)
                    female = 1 ;
                else
                    female = 0 ;
            }
        });
        bt_ok_gender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                restore = true;
                preload = false;
                itemId = 0;
                userId = 0;
                itemCount = 0;
                search_gender = -1;
                preload_gender = -1;

                if(male == 1 && female ==1) {
                    genderType = "all";
                    Log.e("QP","gender : "+genderType);
                    genderDialog.dismiss();
                    getSearchData(genderType);
                }
                else if(male == 1) {
                    genderType = "male";
                    Log.e("QP","gender : "+genderType);
                    genderDialog.dismiss();
                    getSearchData(genderType);
                }
                else if (female == 1) {
                    genderType = "female";
                    Log.e("QP","gender : "+genderType);
                    genderDialog.dismiss();
                    getSearchData(genderType);
                }
                else
                    Toast.makeText(getContext(),getString(R.string.pleaseChooseFilterType),Toast.LENGTH_LONG).show();
            }
        });
    } // function of initGenderDialog

    public void getSearchData(final String gender_type) {

        if(itemsList.size() > 0) {
            itemsList.clear();
            itemsAdapter.notifyDataSetChanged();
        }

        flag = 2 ;
        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_APP_SEARCH, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            if (!loadingMore) {

                                itemsList.clear();
                            }

                            arrayLength = 0;

                            if (!response.getBoolean("error")) {

                                itemCount = response.getInt("itemCount");
                                oldQuery = response.getString("query");
                                userId = response.getInt("itemId");

                                if (response.has("items")) {

                                    JSONArray usersArray = response.getJSONArray("items");

                                    arrayLength = usersArray.length();


                                    if (arrayLength > 0) {

                                        if(itemsList.size() > 0) {
                                            itemsList.clear();
                                            itemsAdapter.notifyDataSetChanged();
                                        }
                                        for (int i = 0; i < usersArray.length(); i++) {

                                            JSONObject profileObj = (JSONObject) usersArray.get(i);

                                            User u = new User(profileObj);

                                            if(gender_type.equals("all")) {
                                                itemsList.add(u);
                                                Log.e("QP","all"+u.getSex());
                                            }
                                            else if (gender_type.equals("male"))
                                            {
                                                if(u.getSex() == 0) {
                                                    itemsList.add(u);
                                                    Log.e("QP", "male" + u.getSex());
                                                }
                                            }
                                            else if (gender_type.equals("female"))
                                            {
                                                if(u.getSex() == 1) {
                                                    itemsList.add(u);
                                                    Log.e("QP", "female" + u.getSex());
                                                }
                                            }
                                            itemsAdapter.notifyDataSetChanged();

                                        }
                                    }
                                }
                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        }
                        finally {

                            loadingComplete();

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(getActivity(), getString(R.string.error_data_loading), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("query", currentQuery);
                params.put("userId", Integer.toString(userId));
                params.put("gender", Integer.toString(search_gender));
                params.put("online", Integer.toString(search_online));

                return params;
            }
        };

        App.getInstance().addToRequestQueue(jsonReq);
    }

    private void filterWithGender()
    {
        genderDialog.show();
    } // function of filterWithGender

    public void onChangeDistance(int position) {

        switch (position) {

            case 0: {

                distance = 50;

                itemId = 0;

              search();

                break;
            }

            case 1: {

                distance = 100;

                itemId = 0;

                search();

                break;
            }

            case 2: {

                distance = 250;

                itemId = 0;

                search();

                break;
            }

            case 3: {

                distance = 500;

                itemId = 0;

                search();

                break;
            }

            case 4: {

                distance = 1000;

                itemId = 0;

                search();

                break;
            }

            default: {

                distance = 50;

                itemId = 0;

                search();

                break;
            }
        }
    }
}