package com.coofee.dep.demo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

const val BACK_URL = "${BuildConfig.SCHEME}://xxx/xxx?type="

fun appendBackUrl(protocol: String, type: String): String {
    return "$protocol&backurl=$BACK_URL$type"
}

const val PROTOCOL_NATIVE =
    "wbutown://jump/town/utowncategoryplus?params=%7b%22url%22%3a%22https%3a%2f%2ftzcapp.58.com%2fcatelist%2ficon%3ftabKey%3dhouse%26subTabKey%3dhouserental%22%2c%22tabkey%22%3a%22house%22%2c%22subtabkey%22%3a%22houserental%22%2c%22from%22%3a%221%22%2c%22title%22%3a%22%e7%a7%9f%e6%88%bf%22%7d"

const val PROTOCOL_WEB =
    "wbutown://jump/town/common?params=%7B%22ailog%22%3A%22%7B%5C%22abtestno%5C%22%3A%5C%22D_house-recall-strategy-1%7CE_fusion-strategy-2%7CF_itemhot-strategy-1%7CG_rank-strategy-3%7CA_smartrecommend-1%7CB_news-recall-strategy-1%7CC_job-recall-strategy-1%7CH_profile-strategy-1%7CI_scatter-strategy-2%5C%22%2C%5C%22channel%5C%22%3A%5C%221%5C%22%2C%5C%22cityId%5C%22%3A0%2C%5C%22countryId%5C%22%3A0%2C%5C%22datasource%5C%22%3A1%2C%5C%22extendJson%5C%22%3A%5C%22%7B%5C%5C%5C%22recallNumbers%5C%5C%5C%22%3A%5B2%2C50%5D%7D%5C%22%2C%5C%22feedtab%5C%22%3A%5C%22recommend%5C%22%2C%5C%22flag%5C%22%3A1%2C%5C%22infoQueryType%5C%22%3A1%2C%5C%22itemType%5C%22%3A0%2C%5C%22itemid%5C%22%3A%5C%22100812280%5C%22%2C%5C%22predictno%5C%22%3A11%2C%5C%22productType%5C%22%3A1%2C%5C%22profileFlag%5C%22%3A1%2C%5C%22publishType%5C%22%3A2%2C%5C%22recallno%5C%22%3A2%2C%5C%22seqno%5C%22%3A%5C%22d0eb9dc4-c6c6-445b-9598-938880dc970f%5C%22%2C%5C%22townId%5C%22%3A0%7D%22%2C%22title%22%3A%22%E8%AF%A6%E6%83%85%22%2C%22url%22%3A%22https%3A%2F%2Ftznew.58.com%2Fview%2Fc%2FsharingDetailNew%3Finfoid%3D1063457161493368832%26oldInfoid%3D100812280%26detailFrom%3D4%26sharesource%3Dtcrecommendshare%26fromFeed%3D1%26source%3D17%22%7D&isFinish=false&needLogin=false"

const val PROTOCOL_RN =
    "wbutown://jump/town/RN?params=%7B%22pagetype%22%3A%22RN%22%2C%22hideBar%22%3A0%2C%22bundleid%22%3A%22214%22%2C%22params%22%3A%7B%22ailog%22%3A%22%22%2C%22cateId%22%3A%2239%22%2C%22detailFrom%22%3A5%2C%22hideBar%22%3A1%2C%22infoId%22%3A%2235122187141036%22%2C%22infoType%22%3A%22tccommoninfo%22%2C%22localId%22%3A%22110105000000%22%2C%22tabKey%22%3A%22shandgood%22%2C%22title%22%3A%22%E8%AF%A6%E6%83%85%22%2C%22tzPage%22%3A%22shandgood%22%2C%22tzdetailType%22%3A%22msgdetail%22%7D%2C%22show_error_navi%22%3Atrue%7D&isFinish=false&needLogin=false"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        open_town_native.setOnClickListener {
            open(appendBackUrl(PROTOCOL_NATIVE, "native"))
        }

        open_town_web.setOnClickListener {
            open(appendBackUrl(PROTOCOL_WEB, "web"))
        }

        open_town_rn.setOnClickListener {
            open(appendBackUrl(PROTOCOL_RN, "rn"))
        }

        dispatch(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        dispatch(intent)
    }

    override fun onResume() {
        super.onResume()

        dispatch(intent)
    }

    private fun dispatch(intent: Intent?) {
        val uri = intent?.data ?: return
        back_url.text = uri.toString()
    }

    private fun open(protocol: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(protocol))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startActivity(intent)
    }
}
