package com.geniteam.adeeb.mapfrag;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ae.lateston.uaecompanies.AppBase;
import ae.lateston.uaecompanies.base.BaseFragment;
import ae.lateston.uaecompanies.databinding.FragCompDetailBinding;
import ae.lateston.uaecompanies.model.Category;
import ae.lateston.uaecompanies.model.CategoryItem;
import ae.lateston.uaecompanies.model.Company;
import ae.lateston.uaecompanies.model.CompanyDetails;
import ae.lateston.uaecompanies.model.Fav;
import ae.lateston.uaecompanies.model.Product;
import ae.lateston.uaecompanies.model.Product2;
import ae.lateston.uaecompanies.model.favmodel.FavModel;
import ae.lateston.uaecompanies.model.login.LoginModel;
import ae.lateston.uaecompanies.model.ratingmodel.Ratingmodel;
import ae.lateston.uaecompanies.model.reviewsmodel.ReviewsModel;
import ae.lateston.uaecompanies.model.reviewsmodel.UserReview;
import ae.lateston.uaecompanies.utils.AppConst;
import ae.lateston.uaecompanies.utils.AppPref;
import ae.lateston.uaecompanies.utils.CommonUtils;
import ae.lateston.uaecompanies.utils.HideShow;
import ae.lateston.uaecompanies.utils.MyDialog;
import ae.lateston.uaecompanies.widgets.CustomFrameLayout;
import ae.lateston.uaecompanies.ws.GetCompaniesDetailsAd;
import ae.lateston.uaecompanies.ws.GetFavourites;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static ae.lateston.uaecompanies.AppBase.service;


public class CompDetailFrag extends BaseFragment implements View.OnClickListener, OnMapReadyCallback, MyDialog.OnDialogFragmentClickListener {

    private FragCompDetailBinding binding;
    private GoogleMap map;
    private boolean isInitial = true;

    int ratingcount = 0;
    private CustomFrameLayout.OnTouchListener listener;
    private String[] contPermission = new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS};
    private View.OnClickListener productClickListener;
    public ArrayList<UserReview> reviewArrayList;
    private String companyNumber;
    private  boolean isloadedFromHomeFrag=false;

    @Override
    public String getTagText() {
        return null;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reviewArrayList = new ArrayList<>();


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (binding == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.frag_comp_detail, container, false);
            binding.setIsCompDetail(true);
            binding.setIsMapLock(true);



            if (getArguments() != null && getArguments().containsKey(AppConst.keyCompany) && getArguments().getParcelable(AppConst.keyCompany) instanceof Company) {
                binding.setCompany((Company) getArguments().getParcelable(AppConst.keyCompany));
            } else {
                binding.setCompany(new Company());
            }
            binding.executePendingBindings();
            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.include.toolbar);
            binding.include.toolbar.setNavigationIcon(R.drawable.ic_back);
            binding.include.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hostActivityInterface.popBackStack();
                }
            });
            // Gets the MapView from the XML layout and creates it
            binding.common.mapView.onCreate(savedInstanceState);
            binding.common.mapView.getMapAsync(this);
        }

        if (reviewArrayList.size()<1) {
            getReviews(binding.getCompany().getId());
        }

        if (binding.getCompany().getCustomerRating()>0){
            binding.rate.btnFirstReview.setText(R.string.click_to_review);
        }


        if(getArguments() != null && getArguments().containsKey(AppConst.keyLoadCompanyDetailFromHome) &&
                getArguments().containsKey(AppConst.keyCompanyNumber)){
            isloadedFromHomeFrag=true;
            companyNumber=getArguments().getString(AppConst.keyCompanyNumber);
        }

        return binding.getRoot();



    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        if(isloadedFromHomeFrag&&companyNumber!=null){
            Log.i("debug","getCompanies");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i("debug","companyNumber "+companyNumber);
                    getCompanyDetails(companyNumber);
                    companyNumber=null;
                }
            }, 10);
        }


        if (isInitial) {
            isInitial = false;

            productClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        int pos = (int) view.getTag(R.id.tag_pos);
                        Log.d("position", "is " + pos);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(AppConst.keyProduct, binding.getCompany().getProducts().get(pos));
                        BaseFragment fragment = new ProdServiceFrag();
                        fragment.setArguments(bundle);
                        hostActivityInterface.addFragment(fragment, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            LayerDrawable stars = (LayerDrawable) binding.rate.rbRating.getProgressDrawable();
            stars.getDrawable(2).setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);
            stars.getDrawable(0).setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorRatingNormal), PorterDuff.Mode.SRC_ATOP);
            stars.getDrawable(1).setColorFilter(ContextCompat.getColor(getActivity(), R.color.colorRatingNormal), PorterDuff.Mode.SRC_ATOP);


            binding.scrollView.setSmoothScrollingEnabled(true);
            binding.laytSelector.tabCompany.setOnClickListener(this);
            binding.laytSelector.tabProduct.setOnClickListener(this);

            binding.rate.btnFirstReview.setOnClickListener(this);
            binding.rate.btnViewAll.setOnClickListener(this);
            //binding.rate.btnLetKnow.setOnClickListener(this);
            binding.rate.btnSubmit.setOnClickListener(this);
            binding.laytRateReview.setOnClickListener(this);
            binding.laytSaveContact.setOnClickListener(this);
            binding.laytShare.setOnClickListener(this);
            binding.laytFav.setOnClickListener(this);
            binding.common.ibLock.setOnClickListener(this);
            binding.common.tvEmail.setOnClickListener(this);
            binding.common.tvMob.setOnClickListener(this);
            binding.common.tvPhone.setOnClickListener(this);
            binding.common.tvWeb.setOnClickListener(this);
            binding.common.lockView.setOnClickListener(this);
            //binding.rate.etMsg.setImeOptions(EditorInfo.IME_ACTION_GO);
            binding.rate.etMsg.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            binding.rate.etMsg.setImeOptions(EditorInfo.IME_ACTION_SEND);
            binding.rate.etMsg.setImeActionLabel(getString(R.string.submit), EditorInfo.IME_ACTION_SEND);


            binding.rate.etMsg.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        hostActivityInterface.hideBoard();
                        sendFeedBack();
                        return true;
                    }
                    return false;
                }
            });

            listener = new CustomFrameLayout.OnTouchListener() {
                @Override
                public void onTouch() {
                    binding.scrollView.requestDisallowInterceptTouchEvent(true);
                }
            };

            new HideShow().setupUI(binding.parent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    ActivityCompat.requestPermissions(getActivity(), contPermission,
                            AppConst.REQ_CONTACT);
                    return;
                }
            }
            setContactStatus();
        }







    }


    @Override
    public void onClick(View view) {

        hostActivityInterface.hideBoard();
        try {
            if (view == binding.laytFav) {

                if (hostActivityInterface.getUser() == null) {
                    showDialog(getString(R.string.error_login_fav), AppConst.AlertType.LOGIN_ERROR);
                } else {
                    if (CommonUtils.isNetworkAvailable(getActivity())) {
                        Map<String, String> map = new HashMap<>();
                        if (binding.getCompany().getIsFav() == 0) {
                            map.put(AppConst.Add_or_Delete, AppConst.GET_ADD_FAVORITES);
                        } else {
                            map.put(AppConst.Add_or_Delete, AppConst.GET_DELETE_FAV);
                        }
                        map.put(AppConst.UserID, hostActivityInterface.getUser().getUserID().toString());
                        map.put(AppConst.U_Email_fav, hostActivityInterface.getUser().getUEmail());
                        map.put(AppConst.CompanyNumber, binding.getCompany().getId());
                        postMyFavStatus(map);
                    } else {
                        showDialog(getString(R.string.msg_check_internet), AppConst.AlertType.ERROR);
                    }
                }

            } else if (view == binding.laytSelector.tabCompany) {
                float destX = ViewHelper.getX(binding.laytSelector.tabCompany);
                ObjectAnimator shiftLeft = ObjectAnimator.ofFloat(binding.laytSelector.movableView, "x",
                        destX);
                shiftLeft.setDuration(150);
                shiftLeft.start();
                binding.setIsCompDetail(true);
            } else if (view == binding.laytSelector.tabProduct) {
                float destX = ViewHelper.getX(binding.laytSelector.tabProduct);
                ObjectAnimator shiftRight = ObjectAnimator.ofFloat(binding.laytSelector.movableView, "x",
                        destX);
                shiftRight.setDuration(150);
                shiftRight.start();
                binding.setIsCompDetail(false);
                generateView();
            } else if (view == binding.rate.btnFirstReview) {
                if (hostActivityInterface.getUser() == null) {
                    showDialog(getString(R.string.error_login_review), AppConst.AlertType.LOGIN_ERROR);
                } else {
                    binding.rate.btnFirstReview.setVisibility(View.GONE);
                    binding.rate.laytReview.setVisibility(View.VISIBLE);
                    binding.scrollView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    }, 50);
                }

            } else if (view == binding.rate.btnViewAll) {
                if (binding.getCompany() != null) {
                    Bundle bundle = new Bundle();
                    List<UserReview> reviews = binding.getReviews().getUserReviews();

                    bundle.putParcelableArrayList(AppConst.keyReview, (ArrayList<? extends Parcelable>) binding.getReviews().getUserReviews());
                    bundle.putInt(AppConst.keyTotal, binding.getReviews().getUserReviews().size()==0 ? 0 : binding.getReviews().getUserReviews().size());
                    bundle.putFloat(AppConst.keyRating, binding.getCompany().getRating());
                    BaseFragment fragment = new ReviewsFrag();
                    fragment.setArguments(bundle);
                    hostActivityInterface.addFragment(fragment, true);
                } else {
                    showDialog(getString(R.string.msg_content_unavailable), AppConst.AlertType.ERROR);
                }
            } else if (view == binding.laytShare) {
                if (binding.getCompany() != null) {

                    String url = binding.getCompany().getUrl();
                    if (url == null){
                        url = "www.uaecompanies.ae";
                    }
                    if (url.startsWith("www", 0)) {
                        url = url.replaceFirst("www", "http://www");
                    }
                    if (URLUtil.isNetworkUrl(url)) {
                        BaseFragment fragment = new SocialShareFrag();
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(AppConst.keyCompany, binding.getCompany());
                        fragment.setArguments(bundle);
                        hostActivityInterface.addFragment(fragment, true);
                        return;
                    }
                }
                showDialog(getString(R.string.msg_content_unavailable), AppConst.AlertType.ERROR);

            } else if (view == binding.laytRateReview) {
                if (hostActivityInterface.getUser() == null) {
                    showDialog(getString(R.string.error_login_fav), AppConst.AlertType.LOGIN_ERROR);
                } else {
                    if (binding.rate.laytLastReview.getVisibility() == View.VISIBLE) {
                        binding.rate.laytReview.setVisibility(binding.rate.laytReview.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    } else {
                        if (binding.rate.btnFirstReview.getVisibility() == View.VISIBLE) {
                            binding.rate.btnFirstReview.setVisibility(View.GONE);
                            binding.rate.laytReview.setVisibility(View.VISIBLE);
                        } else {
                            binding.rate.btnFirstReview.setVisibility(View.VISIBLE);
                            binding.rate.laytReview.setVisibility(View.GONE);
                        }

                    }
                    binding.scrollView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    }, 50);
                }
            } else if (view == binding.common.ibLock) {

                if (binding.getIsMapLock()) {
                    binding.common.frameMap.setListener(listener);
                    binding.setIsMapLock(false);
                    Log.d("listener", "added");
                } else {
                    binding.common.frameMap.setListener(null);
                    binding.setIsMapLock(true);
                    Log.d("listener", "removed");
                }
                setMapGesture();
            } else if (view == binding.rate.btnSubmit) {
                sendFeedBack();
            } else if (view == binding.common.tvEmail) {

                try {
                    if (binding.getCompany() != null && CommonUtils.isEmailValid(binding.getCompany().getEmail())) {
                        Intent emailIntent = new Intent(Intent.ACTION_VIEW);
                        emailIntent.setData(Uri.parse("mailto:".concat(binding.getCompany().getEmail())));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Enquiry via the UAE Companies app");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
                        startActivity(emailIntent);
                    } else {
                        showDialog(getString(R.string.msg_content_unavailable), AppConst.AlertType.ERROR);
                    }

                } catch (ActivityNotFoundException ex) {
                    showDialog(getString(R.string.msg_no_email_clients), AppConst.AlertType.ERROR);
                }

            } else if (view == binding.common.tvMob) {
                if (binding.getCompany() != null && binding.getCompany().getMobile() != null && !binding.getCompany().getMobile().trim().isEmpty()) {
                    callNumber(binding.getCompany().getMobile());
                } else {
                    showDialog(getString(R.string.msg_content_unavailable), AppConst.AlertType.ERROR);
                }
            }

            else if (view == binding.common.lockView){
                try {
                    Bundle bundle = new Bundle();
                    BaseFragment fragment = new FullMapFrag();
                    bundle.putParcelable(AppConst.keyCompany, binding.getCompany());
                    fragment.setArguments(bundle);
                    hostActivityInterface.addFragment(fragment, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else if (view == binding.common.tvPhone) {
                if (binding.getCompany() != null && binding.getCompany().getPhone() != null && !binding.getCompany().getPhone().trim().isEmpty()) {
                    callNumber(binding.getCompany().getPhone());
                } else {
                    showDialog(getString(R.string.msg_content_unavailable), AppConst.AlertType.ERROR);
                }
            } else if (view == binding.common.tvWeb) {
                if (binding.getCompany() != null) {
                    String url = "";
                    if (binding.getCompany().getUrl()!=null) {
                         url = binding.getCompany().getUrl();
                        if (url.startsWith("www", 0)) {
                            url = url.replaceFirst("www", "http://www");
                        }
                    }
                    if (URLUtil.isNetworkUrl(url)) {
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(AppConst.isWeb, true);
                        bundle.putString(AppConst.link, url);
                        BaseFragment fragment = new CommonContentFrag();
                        fragment.setArguments(bundle);
                        hostActivityInterface.addFragment(fragment, true);
                    } else {
                        showDialog(getString(R.string.msg_invalid_url), AppConst.AlertType.ERROR);
                    }
                } else {
                    showDialog(getString(R.string.msg_content_unavailable), AppConst.AlertType.ERROR);
                }
            } else if (view == binding.common.tvLocation) {

                if (hostActivityInterface.getMyLocation() != null) {

                } else {

                }
                /*if (binding.getCompany() != null ) {
                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?saddr="
                                    + mainInterface.getMyLocation().getLatitude()
                                    + ","
                                    + mainInterface.getMyLocation().getLongitude()
                                    + "&daddr="
                                    + binding.getCompany().getLatitude()
                                    + ","
                                    + binding.getCompany().getLongitude()
                            ));
                    intent.setClassName("com.google.android.apps.maps",
                            "com.google.android.maps.MapsActivity");
                    getActivity().startActivity(intent);
                }else{
                    showDialog(getString(R.string.msg_content_unavailable), AppConst.AlertType.ERROR);
                }*/
            } else if (view == binding.laytSaveContact) {
                if (binding.getCompany() != null) {
                    if (binding.getCompany() != null && contactExists(binding.getCompany().getPhone())) {
                        showDialog(getString(R.string.alreadysaved), AppConst.AlertType.SIMPLE);
                    }

                    else {

                        addContact();
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void setContactStatus() {
        if (binding.getCompany() != null && contactExists(binding.getCompany().getPhone())) {
            binding.tvContStatus.setText(getString(R.string.saved));
           // binding.laytSaveContact.setEnabled(false);



        } else {
            binding.tvContStatus.setText(getString(R.string.save));
            //binding.laytSaveContact.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == AppConst.REQ_CONTACT) {
            // We have requested multiple permissions for contacts, so all of them need to be checked.
            if (CommonUtils.verifyPermissions(grantResults)) {
                // All required permissions have been granted, display contacts fragment.
                addContact();

            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        permissions[0]);
                if (!showRationale) {
                    showDialog(getString(R.string.allow_cont), AppConst.AlertType.PERMISSION_ERROR);
                }
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public boolean contactExists(String number) {

        if (number != null) {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            String[] mPhoneNumberProjection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER, ContactsContract.PhoneLookup.DISPLAY_NAME};
            Cursor cur = getActivity().getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);
            try {
                if (cur != null && cur.moveToNext()) {
                    return true;
                }
            } finally {
                if (cur != null)
                    cur.close();
            }
            return false;
        } else {
            return false;
        }
    }

    private void addContact() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                ActivityCompat.requestPermissions(getActivity(), contPermission,
                        AppConst.REQ_CONTACT);
                return;
            }
        }

        if (binding.getCompany() != null) {

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            //------------------------------------------------------ Names
            if (binding.getCompany().getNameEn() != null) {
                ops.add(ContentProviderOperation.newInsert(
                        ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(
                                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                binding.getCompany().getNameEn()).build());
            }

            //------------------------------------------------------ Mobile Number
            if (binding.getCompany().getMobile() != null) {
                ops.add(ContentProviderOperation.
                        newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, binding.getCompany().getMobile())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build());
            }

            //------------------------------------------------------ Home Numbers
           /* if (binding.getCompany().getPhone() != null) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, HomeNumber)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                        .build());
            }*/

            //------------------------------------------------------ Work Numbers
            if (binding.getCompany().getPhone() != null) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, binding.getCompany().getPhone())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                        .build());
            }

            //------------------------------------------------------ Email
            if (binding.getCompany().getEmail() != null) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, binding.getCompany().getEmail())
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                        .build());
            }

            //------------------------------------------------------ Organization
            /*if (!company.equals("") && !jobTitle.equals("")) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, binding.getCompany().get)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                        .build());
            }*/

            // Asking the Contact provider to create a new contact
            try {
                getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                setContactStatus();
                showDialog(getString(R.string.saved_contact), AppConst.AlertType.SUCCESS);
            } catch (Exception e) {
                e.printStackTrace();
                showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
            }
        }
    }

    private void callNumber(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            if (number.contains(",")) {
                String[] mobile = number.split(",");
                if (mobile.length > 0) {
                    intent.setData(Uri.parse("tel:".concat(mobile[0].trim())));
                } else {
                    intent.setData(Uri.parse("tel:".concat(number)));
                }
            } else {
                intent.setData(Uri.parse("tel:".concat(number)));
            }
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFeedBack() {

        try {
            if (hostActivityInterface.getUser() == null) {
                showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                return;
            }

            if (binding.rate.rbRating.getRating() == 0 && binding.rate.etMsg.length() == 0) {
                showDialog(getString(R.string.error_sel_rate_write_review), AppConst.AlertType.ERROR);
                return;
            }
            if (binding.rate.rbRating.getRating() < 1) {
                showDialog(getString(R.string.error_sel_rating), AppConst.AlertType.ERROR);
                return;
            }
            if (binding.rate.etMsg.getText().toString().trim().length() == 0) {
                showDialog(getString(R.string.error_enter_feedback), AppConst.AlertType.ERROR);
                return;
            }

            if (!CommonUtils.isNetworkAvailable(getActivity())) {
                showDialog(getString(R.string.msg_check_internet), AppConst.AlertType.ERROR);
                return;
            }

            Map<String, String> map = new HashMap<>();
            ratingcount = (int) binding.rate.rbRating.getRating();
            map.put(AppConst.Feedback_Rating, String.valueOf(ratingcount));
            map.put(AppConst.CustomerID, hostActivityInterface.getUser().getUserID().toString());
            map.put(AppConst.CompanyNumber, binding.getCompany().getId());
            map.put(AppConst.Feedback_Message, binding.rate.etMsg.getText().toString().trim());

            postUserReview(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postMyFavStatus(Map<String, String> map) {

        binding.setCanShowLoader(true);
        final Call<FavModel> favCall = service.postFavourites(map);
        favCall.enqueue(new Callback<FavModel>() {
            @Override
            public void onResponse(Call<FavModel> call, Response<FavModel> response) {

                try {
                    FavModel status = response.body();
                    if (status != null) {
                        if (status.getSuccess()) {
                            if (binding.getCompany().getIsFav() == 1) {
                                binding.getCompany().setIsFav(0);
                                hostActivityInterface.setFavCount(-1, false);
                                String favcountstr = AppPref.getStringByKey("favcount");
                                int countless =  Integer.parseInt(favcountstr) -1;
                                hostActivityInterface.getUser().setFavouritesCount(countless);
                                AppPref.setStringInPreferences("favcount", String.valueOf(countless));
                                showDialog(status.getContent().getMessage(), AppConst.AlertType.SUCCESS);

                            } else {
                                binding.getCompany().setIsFav(1);
                                hostActivityInterface.setFavCount(1, false);
                                String favcountstr = AppPref.getStringByKey("favcount");
                                int countless =  Integer.parseInt(favcountstr) + 1;
                                hostActivityInterface.getUser().setFavouritesCount(countless);
                                AppPref.setStringInPreferences("favcount", String.valueOf(countless));
                                showDialog(status.getContent().getMessage(), AppConst.AlertType.SUCCESS);
                            }

                        } else {
                            showDialog(status.getContent().getMessage(), AppConst.AlertType.ERROR);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                }

                binding.setCanShowLoader(false);
            }

            @Override
            public void onFailure(Call<FavModel> call, Throwable t) {

                showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                binding.setCanShowLoader(false);
            }
        });
    }

    private void postUserReview(Map<String, String> map) {

        binding.setCanShowLoader(true);
        Call<Ratingmodel> review = service.postFeedback(map);
        review.enqueue(new Callback<Ratingmodel>() {
            @Override
            public void onResponse(Call<Ratingmodel> call, Response<Ratingmodel> response) {

                try {
                    Ratingmodel status = response.body();
                    if (status != null) {
                        if (status.getSuccess()) {
                            getReviews(binding.getCompany().getId());
                            binding.rate.rbRating.setRating(0);
                            binding.rate.etMsg.setText("");
                            binding.rate.laytReview.setVisibility(View.GONE);
                            binding.rate.btnFirstReview.setText(R.string.click_to_review);
                            int cusrating = binding.getCompany().getCustomerRating();
                            binding.getCompany().setCustomerRating(cusrating+1);
                            showDialog(status.getContent().getMessage(), AppConst.AlertType.SUCCESS);
                            if (binding.getCompany().getReview() != null && binding.getCompany().getReview().size() == 0) {
                                binding.rate.btnFirstReview.setVisibility(View.VISIBLE);
                            }
                        } else {
                            showDialog(status.getContent().getMessage(), AppConst.AlertType.ERROR);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                }

                binding.setCanShowLoader(false);
            }

            @Override
            public void onFailure(Call<Ratingmodel> call, Throwable t) {

                showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                binding.setCanShowLoader(false);
            }
        });
    }

    private void getAllFav(Map<String, String> map) {

        binding.setCanShowLoader(true);
        Call<GetFavourites> favCall = service.getAllFavourites(map);
        favCall.enqueue(new Callback<GetFavourites>() {
            @Override
            public void onResponse(Call<GetFavourites> call, Response<GetFavourites> response) {

                try {
                    GetFavourites status = response.body();
                    if (status != null) {
                        if (status.state) {
                            if (status.content != null && status.content.size() > 0) {
                                if (isFavCompOfUser(status.content)) {
                                    binding.getCompany().setIsFav(1);
                                } else {
                                    binding.getCompany().setIsFav(0);
                                }
                            }
                        } else {
                            showDialog(status.error.getMessage(), AppConst.AlertType.ERROR);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                }

                binding.setCanShowLoader(false);
            }

            @Override
            public void onFailure(Call<GetFavourites> call, Throwable t) {

                if (getActivity() != null) {
                    showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                    binding.setCanShowLoader(false);
                }
            }
        });
    }

    private boolean isFavCompOfUser(List<Fav> list) {
        String id = hostActivityInterface.getUser().getUserID().toString();
        for (Fav fav : list) {
            if (fav.getCustomerId().equals(id)) {
                return true;
            }

        }
        return false;
    }

    private void showDialog(String msg, int alertType) {
        hostActivityInterface.hideBoard();
        if (getActivity() != null) {
            MyDialog dialog = new MyDialog(getActivity(), this, alertType, msg);
            dialog.show();
        }
    }

    private void setMapGesture() {
        if (map != null) {
            map.getUiSettings().setAllGesturesEnabled(!binding.getIsMapLock());
        }
    }

    @Override
    public void onResume() {
        try {
            if (binding.common.mapView != null) {
                binding.common.mapView.onResume();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (binding.common.mapView != null) {
                binding.common.mapView.onPause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (binding.common.mapView != null) {
                binding.common.mapView.onDestroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getTargetRequestCode() != 0) {
            getTargetFragment().onActivityResult(getTargetRequestCode(),
                    Activity.RESULT_OK, new Intent().putExtra(AppConst.idParam, binding.getCompany().getId()).putExtra(AppConst.keyPos, getArguments().getInt(AppConst.keyPos)));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConst.REQ_LOGIN) {
                if (hostActivityInterface.getUser() != null) {
                    Map<String, String> map = new HashMap<>();
                    map.put(AppConst.serviceType, AppConst.GET_FAVORITES);
                    map.put(AppConst.searchId, hostActivityInterface.getUser().getUserID().toString());//search with customer id
                    map.put(AppConst.searchIn, "1");//1 refer to search in customer
                    getAllFav(map);
                    setContactStatus();
                }
            }
        }

    }

    @BindingAdapter("bind:categories")
    public static void setCategories(TextView textView, List<CategoryItem> items) {
        try {
            if (items == null)
                return;
            String prefix = "";
            StringBuilder csvList = new StringBuilder();
            for (CategoryItem s : items) {
                csvList.append(prefix);
                csvList.append(s.getCategoryNameEn());
                prefix = ", ";
            }
            textView.setText(csvList.toString().trim().toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            if (binding.common.mapView != null) {
                binding.common.mapView.onLowMemory();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void generateView() {
        try {
            binding.laytProd.removeAllViewsInLayout();
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams parentParams2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);

            LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, 35 * (int) getResources().getDisplayMetrics().density, 1f);
            parentParams.setMargins(0, 0, 0, 0);


            LinearLayout layoutParent = new LinearLayout(getActivity());
            LinearLayout layoutline = new LinearLayout(getActivity());
            //LinearLayout layoutChild;
            Button button;

            /*try {
                Branches branches = new Branches();
                branches.setId(merchant.getId());
                branches.setMallName(merchant.getMallName());
                branches.setPhone(merchant.getPhone());
                merchant.getBranches().add(0, branches);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int count = merchant.getBranches().size();*/

            if (binding.getCompany().getProducts() == null || binding.getCompany().getProducts().size() == 0) {
                binding.laytProdContainer.setBackgroundColor(0);
                return;
            }
            int size = binding.getCompany().getProducts().size();
            for (int s = 0; s < size; s++) {
                //layoutChild = new LinearLayout(getActivity());
                button = new Button(getActivity());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    button.setStateListAnimator(null);
                }
                button.setTag(R.id.tag_pos, s);
              //  if (AppPref.getLanguage().equalsIgnoreCase("en")) {
                    button.setText(binding.getCompany().getProducts().get(s).getNameEn());
               // }else {
                 //   button.setText(binding.getCompany().getProducts().get(s).getNameAr());

               // }



                ///if (s % 3 == 0) {
                    //create new horizontal parent layout
                    layoutParent = new LinearLayout(getActivity());



                    layoutParent.setOrientation(LinearLayout.HORIZONTAL);
                    layoutParent.setGravity(Gravity.CENTER);
                    layoutParent.setLayoutParams(parentParams);
                  ///  layoutParent.setWeightSum(3);
                    binding.laytProd.addView(layoutParent);

                    //childParams.setMargins(0, 8, 8, 0);
                    tvParams.setMargins(0, 0, 0, 1);

             ///   } else if (s % 3 == 1) {
                    //childParams.setMargins(8, 8,8, 8);
                  ///  tvParams.setMargins(16, 8, 16, 8);
             ///   } else if (s % 3 == 2) {
                    //childParams.setMargins(0, 8, 8, 0);
                 ///   tvParams.setMargins(0, 8, 0, 8);
              ///  }
                //inner layout
                //layoutChild.setOrientation(LinearLayout.VERTICAL);
                // layoutChild.setWeightSum(1);

                // layoutChild.setGravity(Gravity.CENTER);
                // layoutChild.setLayoutParams(childParams);


                button.setBackgroundResource(R.drawable.prod_btn_bg);
                button.setTypeface(AppBase.fontCache.get(getString(R.string.font_name_myriadpro_regular)));
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                button.setTextColor(ContextCompat.getColorStateList(getActivity(), R.color.white));
                button.setGravity(Gravity.CENTER);
                button.setLayoutParams(tvParams);
                button.setPadding(8, 8, 8, 8);
                button.setOnClickListener(productClickListener);
                layoutParent.addView(button);

                //layoutParent.addView(layoutChild);
                //layoutChild.invalidate();
                //layoutParent.invalidate();
               /* if (s >= 0 && s <= 2) {
                    layoutParent.setVisibility(View.VISIBLE);
                } else {
                    layoutParent.setVisibility(View.GONE);
                }*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;

        setMapGesture();

        if (map != null) {
            map.setOnMarkerClickListener(null);
            map.getUiSettings().setMapToolbarEnabled(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (binding.getCompany() != null) {
                        if(binding.getCompany().getLongitude()!=null)
                        if (binding.getCompany().getLatitude().equals("") &&binding.getCompany().getLongitude()!=null|| !binding.getCompany().getLongitude().equals("")) {
                            map.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(binding.getCompany().getLatitude()), Double.parseDouble(binding.getCompany().getLongitude())))
                                    .snippet(binding.getCompany().getNameEn()));
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(binding.getCompany().getLatitude()), Double.parseDouble(binding.getCompany().getLongitude())), 10));
                        }
                    }
                }
            }, 250);


        }

    }

public void displayCompanyLocationOnMap(){
    if (map != null) {
        map.setOnMarkerClickListener(null);
        map.getUiSettings().setMapToolbarEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (binding.getCompany() != null) {
                    if(binding.getCompany().getLongitude()!=null)
                        if (binding.getCompany().getLatitude().equals("") &&binding.getCompany().getLongitude()!=null|| !binding.getCompany().getLongitude().equals("")) {
                            map.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(binding.getCompany().getLatitude()), Double.parseDouble(binding.getCompany().getLongitude())))
                                    .snippet(binding.getCompany().getNameEn()));
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(binding.getCompany().getLatitude()), Double.parseDouble(binding.getCompany().getLongitude())), 10));
                        }
                }
            }
        }, 250);


    }
}
    @Override
    public void onOkClicked(@AppConst.AlertType int alertType, int buttonId) {

        if (buttonId == R.id.btnOk) {
            if (alertType == AppConst.AlertType.LOGIN_ERROR) {
                BaseFragment fragment = new LoginFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean(AppConst.keyReturn, true);
                fragment.setArguments(bundle);
                fragment.setTargetFragment(CompDetailFrag.this, AppConst.REQ_LOGIN);
                hostActivityInterface.addFragment(fragment, true);
            } else if (alertType == AppConst.AlertType.PERMISSION_ERROR) {
                try {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void getReviews(String companyID){

        if (reviewArrayList!=null){
            reviewArrayList.clear();
        }

        Map<String, String> map = new HashMap<>();
        map.put(AppConst.CompanyNumber, companyID);

        Call<ReviewsModel> call = service.getReviews(map);
        call.enqueue(new Callback<ReviewsModel>() {
            @Override
            public void onResponse(Call<ReviewsModel> call, Response<ReviewsModel> response) {
                try {
                    ReviewsModel reviewsModel = response.body();
                    if (reviewsModel.getSuccess()) {
                        for (UserReview review : reviewsModel.getUserReviews()) {
                            String date = review.getDCreatedDate();
                            date = date.replaceAll("\\/","");
                            date = date.replaceAll("Date","");
                            date = date.replaceAll("\\(","");
                            date = date.replaceAll("\\)","");

                            String dateString = convertDate(date,"dd MMM yyyy");
                            review.setDCreatedDate(dateString);
                            reviewArrayList.add(review);

                        }
                        reviewsModel.setUserReviews(reviewArrayList);
                        binding.setReviews(reviewsModel);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                }

                binding.setCanShowLoader(false);
            }

            @Override
            public void onFailure(Call<ReviewsModel> call, Throwable t) {
                binding.setCanShowLoader(false);
            }
        });
    }



    private void getCompanyDetails(String companyNumber) {

        Map<String, String> map = null;
        String customerID="0";
        String userLat="0";
        String userLon="0";
        try {
            if (getActivity() == null)
                return;


            if (!CommonUtils.isNetworkAvailable(getActivity())) {
                showDialog(getString(R.string.msg_check_internet), AppConst.AlertType.ERROR);
                return;
            }
            LoginModel model = AppPref.getUserDataFromPreferences();

            map = new HashMap<>();





            if (hostActivityInterface.getMyLocation() != null) {
               // map.put(AppConst.usrlat, String.valueOf(hostActivityInterface.getMyLocation().getLatitude()));
              // map.put(AppConst.usrlong, String.valueOf(hostActivityInterface.getMyLocation().getLongitude()));

                userLat=hostActivityInterface.getMyLocation().getLatitude()+"";
               userLon=hostActivityInterface.getMyLocation().getLongitude()+"";
            }

            else {
                userLat="0";
                userLon="0";
            }


            Handler handler=new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                }
            },100);




            map.put(AppConst.usrlong,userLon);
            map.put(AppConst.usrlat,userLat);



            map.put(AppConst.customerid, customerID);
            map.put(AppConst.keyCompanyNumber,companyNumber);








            Log.i("debug", " params "+map.toString());
        } catch (Exception e) {
            Log.i("debug"," err "+e);
            e.printStackTrace();
        }


      //  binding.setCanShowLoader(true);

        Call<GetCompaniesDetailsAd> companiesCall = null;

        binding.setCanShowLoader(true);



            companiesCall = service.getCompanyDetails(map);





        companiesCall.enqueue(new Callback<GetCompaniesDetailsAd>() {
            @Override
            public void onResponse(Call<GetCompaniesDetailsAd> call, Response<GetCompaniesDetailsAd> response) {
Log.i("debug","onResponse");
                try {
                    Log.i("debug","error body "+response.errorBody());
                   Log.i("debug","succes "+response.isSuccessful());
                    GetCompaniesDetailsAd companyModel = response.body();
                    Log.i("debug","success "+companyModel.getSuccess());
                    if (companyModel.getCompanyDetails() != null) {
                        //binding.parent.setVisibility(View.VISIBLE);
                       // GetCompanies companies = new GetCompanies();

                        CompanyDetails company = companyModel.getCompanyDetails();
                        Company companyobj = new Company();
                        companyobj.setId(company.getCompanyNumber());
                        if (AppPref.getLanguage().equalsIgnoreCase("en")) {
                            companyobj.setNameEn(company.getVCompanyName());
                            companyobj.setAddressEn(company.getVAddress());
                            companyobj.setDistrictNameEn(company.getAreaNameEn());
                            companyobj.setCityNameEn(company.getCityNameEn());

                        } else {

                            companyobj.setNameEn(company.getVCompanyNameAr());
                            companyobj.setAddressEn(company.getVAddressAr());
                            companyobj.setDistrictNameEn(company.getAreaNameAr());
                            companyobj.setCityNameEn(company.getCityNameAr());
                        }

                        if (company.getLatitude() != null) {
                            companyobj.setLatitude(company.getLatitude());
                        } else {
                            companyobj.setLatitude(CommonUtils.getLatitudeByCityID((company.getCityID())));
                        }

                        if (company.getLongitude() != null) {
                            companyobj.setLongitude(company.getLongitude());
                        } else {
                            companyobj.setLongitude(CommonUtils.getLongitudeByCityID((company.getCityID())));
                        }

                        if(company.getDistance()!=0.0)
                        companyobj.setDistance(Float.parseFloat(CommonUtils.arabicToDecimal(String.valueOf(company.getDistance()))));


                        companyobj.setMobile(company.getVMobile());
                        if(company.getVFaxNo()!=null)
                        companyobj.setFax((String) company.getVFaxNo());
                        if(company.getVLandLineNo()!=null)
                        companyobj.setPhone(company.getVLandLineNo());
                        companyobj.setEmail(company.getVEmail());
                        companyobj.setUrl((String) company.getShareUrl());
                        companyobj.setIsFav((company.getIsFavourite()));
                        companyobj.setUrl(company.getVWebSite());

                        String date = company.getDCreatedDate();
                        date = date.replaceAll("\\/", "");
                        date = date.replaceAll("Date", "");
                        date = date.replaceAll("\\(", "");
                        date = date.replaceAll("\\)", "");
                        companyobj.setdCreatedDate(date);
                        //companyobj.setIsFeatured((Integer) company.getIsFeatured());
//                            companyobj.setMcId("");
//                            if (company.getDistance() != null) {
//                                float distance = (float) company.getDistance();
//                                companyobj.setDistance(distance);
//                            }

                        // companyobj.setpId(Long.parseLong(""));
                        if (company.getDistrictId() != null) {
                            companyobj.setDistrictId(company.getDistrictId());
                        }
                        companyobj.setCountryId(0);
                        companyobj.setCountryNameEn("");
                        if (company.getAverageRating() != null) {
                            Float aver = Float.parseFloat((String) company.getAverageRating());
                            int avernumb = Math.round(aver);
                            companyobj.setRating((float) avernumb);
                        }
                        if (company.getTotalVoters() != null) {
                            companyobj.setCustomerRating(Integer.parseInt((String) company.getTotalVoters()));
                        }

                        companyobj.setLogo(company.getLogoURL());
                        // companyobj.setReview(company.getreview);

                        ArrayList<CategoryItem> arraycat = new ArrayList<>();
                        for (Category category : company.getCategories()) {

                            CategoryItem item = new CategoryItem();
                            item.setCategoryId(category.getCategoryID());
                            if (AppPref.getLanguage().equalsIgnoreCase("en")) {
                                item.setCategoryNameEn(category.getNameE());
                            } else {
                                item.setCategoryNameEn(category.getNameA());
                            }

                            if (category.getCompanyCategoryID() != null) {
                                item.setParentId(Integer.parseInt(String.valueOf(category.getCompanyCategoryID())));
                            }
                            arraycat.add(item);
                        }

                        companyobj.setCategories(arraycat);

                        ArrayList<Product> arrproduct = new ArrayList<>();
                        for (Product2 productobj : company.getProducts()) {

                            Product item = new Product();
                            item.setId(productobj.getCompanyNumber());
                            item.setMerchantId(companyobj.getMcId());

                            if (AppPref.getLanguage().equalsIgnoreCase("en")) {
                                item.setNameEn(productobj.getNameEn());
                                item.setDescriptionEn(productobj.getDescriptionEn());
                             //   item.setImages(productobj.);
                            } else {
                             //   Log.i("debug","arabic "+productobj.getNameAr());
                                item.setNameEn(productobj.getNameAr());
                                item.setDescriptionEn(productobj.getDescriptionAr());
                            }

                            arrproduct.add(item);
                        }

                        companyobj.setProducts(arrproduct);
                        binding.setCompany(companyobj);

                       binding.getCompany().getLatitude();
                        binding.getCompany().getLongitude();
                        binding.executePendingBindings();
                    }
            } catch (Exception e) {

                    Log.i("debug","exception "+e);
                e.printStackTrace();
                showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
            }

displayCompanyLocationOnMap();

                binding.setCanShowLoader(false);


            }

            @Override
            public void onFailure(Call<GetCompaniesDetailsAd> call, Throwable t) {
                binding.setCanShowLoader(false);
                Log.i("debug","fail error 1 "+t);
                try {
                    if (!CommonUtils.isNetworkAvailable(getActivity())) {
                        showDialog(getString(R.string.msg_check_internet), AppConst.AlertType.ERROR);
                    } else {
                        showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    Log.i("debug","fail error2 "+e);
                    e.printStackTrace();
                    showDialog(getString(R.string.error_undone), AppConst.AlertType.ERROR);
                }

            }
        });
    }

    public static String convertDate(String dateInMilliseconds, String dateFormat) {
        return DateFormat.format(dateFormat, Long.parseLong(dateInMilliseconds)).toString();
    }
}
