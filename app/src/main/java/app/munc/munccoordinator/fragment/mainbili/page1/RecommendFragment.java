package app.munc.munccoordinator.fragment.mainbili.page1;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.gson.GsonBuilder;
import com.lcodecore.tkrefreshlayout.IHeaderView;
import com.lcodecore.tkrefreshlayout.OnAnimEndListener;
import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.lcodecore.tkrefreshlayout.utils.DensityUtil;
import com.youth.banner.Banner;
import com.youth.banner.BannerConfig;
import com.youth.banner.Transformer;
import com.youth.banner.listener.OnBannerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import app.munc.munccoordinator.R;
import app.munc.munccoordinator.adapter.homepage.RecommendAdapter;
import app.munc.munccoordinator.content.CommonContent;
import app.munc.munccoordinator.content.UrlBaseContent;
import app.munc.munccoordinator.content.UrlContent;
import app.munc.munccoordinator.fragment.mainbili.page1.web.RecommendWebView;
import app.munc.munccoordinator.info.homepage.IndexInfo;
import app.munc.munccoordinator.inter.ResponseCommonService;
import app.munc.munccoordinator.manager.GlideImageLoader;
import app.munc.munccoordinator.util.AppCompatUtils;
import app.munc.munccoordinator.util.Utils;
import app.munc.munccoordinator.view.BiliBottomView;
import app.munc.munccoordinator.view.BilibiliRefreshView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import me.grantland.widget.AutofitTextView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A simple {@link Fragment} subclass.  推荐模块
 */
public class RecommendFragment extends Fragment implements IHeaderView {


    private static final int SPAN_COUNT_ONE = 1;
    private static final int LOAD_BANNER_SUCESS = 2;
    private static final int LOAD_PRODUCT_SUCESS = 3;
    private static final int LOAD_PRODUCT_SUCESS2 = 4;
    @BindView(R.id.tv_comprehensive)
    AutofitTextView tvComprehensive;
    @BindView(R.id.ll_Ranking_List)
    LinearLayout llRankingList;
    @BindView(R.id.ll_Label)
    LinearLayout llLabel;
    @BindView(R.id.recyclerview)
    RecyclerView recyclerview;
    Unbinder unbinder;
    @BindView(R.id.ll_topMain)
    LinearLayout llTopMain;
    @BindView(R.id.twinklingRefreshLayout)
    TwinklingRefreshLayout twinklingRefreshLayout;
    @BindView(R.id.iv_goTop)
    ImageView ivGoTop;
    private int width;
    private RecommendAdapter mAdapter;
    private List listT = new ArrayList();
    private int totalDy;
    private Banner mBanner;
    private List<String> banner_images = new ArrayList<String>();//本地创建
    private List<IndexInfo.DataBean.BannerItemBean> banner_item; //轮播图列表的集合
    private List<IndexInfo.DataBean> data;//总数据
    private boolean isExecuteRefresh = false;
    private boolean isExecuteLoadMore = false;
    private RelativeLayout rlBanner;
    public static int idx2 = 1503941159;

    List unBannerlist = new ArrayList();//首页列表数据 不包括轮播 下拉用
    List unBannerLoadMorelist = new ArrayList();//首页列表数据 不包括轮播 加载更多

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_BANNER_SUCESS:
                    setBanners();
                    break;
                case LOAD_PRODUCT_SUCESS:
                    if (isExecuteRefresh) {
                        mAdapter.setDatas(unBannerlist);
                        twinklingRefreshLayout.finishRefreshing();
                        isExecuteRefresh = false;
                    } else if (isExecuteLoadMore) {
                        mAdapter.addData(dataLoadMore);
                        twinklingRefreshLayout.finishLoadmore();
                        isExecuteLoadMore = false;
                        if (dataLoadMore.size() == 0) {
                            Utils.showToast(getContext(), getContext().getString(R.string.loadnomore));
                            return;
                        }
                    }
                    break;
                case LOAD_PRODUCT_SUCESS2:
                    if (isExecuteLoadMore) {
                        mAdapter.addData(unBannerLoadMorelist);
                        twinklingRefreshLayout.finishLoadmore();
                        isExecuteLoadMore = false;
                        if (unBannerLoadMorelist.size() == 0) {
                            Utils.showToast(getContext(), getContext().getString(R.string.loadnomore));
                            return;
                        }
                    }
                    break;
            }
        }
    };
    private List<IndexInfo.DataBean> dataLoadMore;


    public RecommendFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recommend, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    private void init() {
        initLayout();
        initSetting();
        resitDataLists();
        isExecuteRefresh = true;
        initRetrofit();
    }

    //首页第一页 带轮播图的
    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(UrlContent.commonApp + UrlBaseContent.Index)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .build();
        ResponseCommonService service = retrofit.create(ResponseCommonService.class);
        Call<IndexInfo> call = service.getIndex(CommonContent.actionKey, CommonContent.appkey, CommonContent.banner_hash, CommonContent.build,
                CommonContent.device, CommonContent.idx, CommonContent.login_event, CommonContent.mobi_app, CommonContent.network
                , CommonContent.open_event, CommonContent.platform, CommonContent.pull, Utils.getUUid(), CommonContent.style, CommonContent.ts);
        call.enqueue(new Callback<IndexInfo>() {
            @Override
            public void onResponse(Call<IndexInfo> call, Response<IndexInfo> response) {
                data = response.body().getData();
                //这里从1开始
                for (int i = 1; i < data.size(); i++) {
                    unBannerlist.add(data.get(i));
                }
                banner_item = response.body().getData().get(0).getBanner_item();
                handler.obtainMessage(LOAD_BANNER_SUCESS, response).sendToTarget();
                handler.obtainMessage(LOAD_PRODUCT_SUCESS, response).sendToTarget();
            }

            @Override
            public void onFailure(Call<IndexInfo> call, Throwable t) {
                Log.e("onFailure", t.getMessage());
            }
        });
    }

    //首页 不带轮播图的
    private void initRetrofit2() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(UrlContent.commonApp + UrlBaseContent.Index)
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                .build();
        ResponseCommonService service = retrofit.create(ResponseCommonService.class);
        Call<IndexInfo> call = service.getIndex2(CommonContent.actionKey, CommonContent.appkey, CommonContent.banner_hash2, CommonContent.build,
                CommonContent.device, idx2 + "", CommonContent.mobi_app, CommonContent.network
                , "", CommonContent.platform, CommonContent.pull2, Utils.getUUid(), CommonContent.style, "15142712" + new Random().nextInt(CommonContent.ts2));
        call.enqueue(new Callback<IndexInfo>() {
            @Override
            public void onResponse(Call<IndexInfo> call, Response<IndexInfo> response) {
                dataLoadMore = response.body().getData();
                if (response.body().getData().get(0).getBanner_item() != null) {
                    //这里从1开始
                    for (int i = 1; i < dataLoadMore.size(); i++) {
                        unBannerLoadMorelist.add(dataLoadMore.get(i));
                    }
                    handler.obtainMessage(LOAD_PRODUCT_SUCESS2, response).sendToTarget();
                } else {
                    handler.obtainMessage(LOAD_PRODUCT_SUCESS, response).sendToTarget();
                }
            }

            @Override
            public void onFailure(Call<IndexInfo> call, Throwable t) {
                Log.e("onFailure", t.getMessage());
            }
        });
    }

    //设置轮播图
    private void setBanners() {
        for (IndexInfo.DataBean.BannerItemBean bannerItemBean : banner_item) {
            String image = bannerItemBean.getImage();
            banner_images.add(image);
        }
        mBanner.setImages(banner_images);
        Log.e("banner_images", banner_images.size() + "");
        mBanner.setOnBannerListener(new OnBannerListener() {
            @Override
            public void OnBannerClick(int position) {
                Intent intent = new Intent(getActivity(), RecommendWebView.class);
                intent.putExtra(CommonContent.WEBVIEW, banner_item.get(position).getUri());
                intent.putExtra(CommonContent.WEBVIEW_TITLE, "");
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
        mBanner.start();
    }

    private void initSetting() {
        BilibiliRefreshView headerView = new BilibiliRefreshView(getActivity());
        twinklingRefreshLayout.setHeaderView(headerView);

        BiliBottomView footView = new BiliBottomView(getActivity());
        twinklingRefreshLayout.setBottomView(footView);

        twinklingRefreshLayout.setEnableOverScroll(true);
        twinklingRefreshLayout.setHeaderHeight(90);
        twinklingRefreshLayout.setMaxHeadHeight(180);
        twinklingRefreshLayout.setBottomHeight(90);
        twinklingRefreshLayout.setMaxBottomHeight(180);
        twinklingRefreshLayout.setAutoLoadMore(true);
        //设置rc
        final GridLayoutManager manager = new GridLayoutManager(getActivity(), SPAN_COUNT_ONE);
        recyclerview.setLayoutManager(manager);
        mAdapter = new RecommendAdapter(getActivity(), listT, getActivity());
        //头部的布局适配
        View inflate = LayoutInflater.from(getActivity()).inflate(R.layout.item_rc_recommend_header, null); //有一个null header布局 如果不想要头的话
        rlBanner = inflate.findViewById(R.id.rl_banner);
        mBanner = inflate.findViewById(R.id.banner);

        mBanner.setIndicatorGravity(BannerConfig.RIGHT);

        AppCompatUtils.setScreenScale(rlBanner, width, 1, 2.5);
        mBanner.setImageLoader(new GlideImageLoader());
        mBanner.setBannerAnimation(Transformer.Default);
        mBanner.setDelayTime(4000);
        mBanner.setIndicatorPadding(DensityUtil.dp2px(getActivity(), 25));

        mAdapter.setHeaderView(inflate); //这里把头部拿出来了
        recyclerview.setAdapter(mAdapter);

        //设置一个头 头部占一行
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (mAdapter.isHeaderView(position)) ? manager.getSpanCount() : 1;
            }
        });

        //设置下拉上拉
        recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalDy = totalDy + dy;
                if (totalDy > 1000) {
                    ivGoTop.setVisibility(View.VISIBLE);
                } else {
                    ivGoTop.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        twinklingRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {

            @Override
            public void onRefresh(TwinklingRefreshLayout refreshLayout) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resitDataLists();
                        isExecuteRefresh = true;
                        initRetrofit();
                    }
                }, 1200);
            }

            @Override
            public void onLoadMore(final TwinklingRefreshLayout refreshLayout) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        idx2 = idx2 - 10;
                        if (unBannerLoadMorelist != null) {
                            unBannerLoadMorelist.clear();
                        }
                        isExecuteLoadMore = true;
                        initRetrofit2();
                    }
                }, 700);
            }
        });
    }


    private void resitDataLists() {
        if (data != null) {
            data.clear();
        }
        if (unBannerlist != null) {
            unBannerlist.clear();
        }
        if (banner_images != null) {
            banner_images.clear();
        }
    }

    private void initLayout() {
        width = AppCompatUtils.getAppScreenWidth(getActivity());
        AppCompatUtils.setScreenScale(llTopMain, width, 1, 8);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.tv_comprehensive, R.id.ll_Ranking_List, R.id.ll_Label, R.id.iv_goTop})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_comprehensive:
                break;
            case R.id.ll_Ranking_List:
                break;
            case R.id.ll_Label:
                break;
            case R.id.iv_goTop:
                ivGoTop.setVisibility(View.GONE);
                recyclerview.scrollBy(0, -totalDy);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //开始轮播
        mBanner.startAutoPlay();
    }

    @Override
    public void onStop() {
        super.onStop();
//        mBanner.stopAutoPlay(); 会出现空白
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBanner.releaseBanner();
        // 避免Handler导致内存泄漏
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    @Override
    public void onPullingDown(float fraction, float maxHeadHeight, float headHeight) {

    }

    @Override
    public void onPullReleasing(float fraction, float maxHeadHeight, float headHeight) {

    }

    @Override
    public void startAnim(float maxHeadHeight, float headHeight) {

    }

    @Override
    public void onFinish(OnAnimEndListener animEndListener) {

    }

    @Override
    public void reset() {

    }
}
