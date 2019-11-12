package com.example.tmdbtop20

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.Cache
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.ImageRequest
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

class MovieDetails : AppCompatActivity() {

    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
    var errorThumbnail: Bitmap? = null
    var cache: Cache? = null

    private fun setBackGround(url:String, altUrl: String?) {
        val network = BasicNetwork(HurlStack())
        val queue: RequestQueue = RequestQueue(cache, network).apply {
            start()
        }
        val backgroundView: ImageView = findViewById(R.id.background)

        val cacheEntry: Cache.Entry? = cache?.get(url)
        if(cacheEntry != null){
            // Load image from cache
            val inputStream = ByteArrayInputStream(cacheEntry.data)
            try {
                val thumbnail = BitmapFactory.decodeStream(inputStream)
                backgroundView.setImageBitmap(thumbnail)
                return
            }catch (e: Exception){
                backgroundView.setImageBitmap(errorThumbnail)
            }
        }

        val imageRequest = ImageRequest(url,
            Response.Listener<Bitmap> { response: Bitmap? ->
                if (response != null) {
                    backgroundView.setImageBitmap(response)
                } else {
                    backgroundView.setImageBitmap(errorThumbnail)
                    if (altUrl != null) {
                        setBackGround(altUrl, null)
                    }
                }
            }, 0, 0, ImageView.ScaleType.FIT_XY, null,
            Response.ErrorListener { error: VolleyError? ->
                backgroundView.setImageBitmap(errorThumbnail)
                if (altUrl != null) {
                    setBackGround(altUrl, null)
                }
            })

        queue.add(imageRequest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)
        val intentString: String? = intent.getStringExtra(MOVIE_INFO)
        if(intentString != null) {
            val jsonObject = JSONObject(intentString)
            errorThumbnail = BitmapFactory.decodeResource(resources, R.drawable.error_thumbnail)

            cache = DiskBasedCache(cacheDir, cacheSize)
            // Configurar descrição do filme
            val description: TextView = findViewById(R.id.description)

            val title = SpannableString(jsonObject["title"].toString())
            title.setSpan(RelativeSizeSpan(2.0f), 0, title.length, 0)
            title.setSpan(StyleSpan(Typeface.BOLD), 0, title.length, 0)
            val date: Calendar = Calendar.getInstance()
            date.time = formatter.parse(jsonObject["release_date"].toString())
            val year = SpannableString("(" + (date.get(Calendar.YEAR)).toString() + ")\n")
            year.setSpan(RelativeSizeSpan(1.75f), 0, year.length, 0)
            year.setSpan(StyleSpan(Typeface.BOLD), 0, year.length, 0)
            year.setSpan(ForegroundColorSpan(Color.GRAY), 0, year.length, 0)
            val duration = SpannableString("Duração: ")
            duration.setSpan(StyleSpan(Typeface.BOLD), 0, duration.length, 0)
            val durationValue = SpannableString(jsonObject["runtime"].toString() + " min\n")
            val genreList: JSONArray = jsonObject.getJSONArray("genres")
            var genreListString = ""
            for(i in 0 until genreList.length()){
                genreListString += genreList[i].toString()
                if(i < genreList.length() - 1){
                    genreListString += ", "
                }
            }
            val genres = SpannableString("Gêneros: ")
            genres.setSpan(RelativeSizeSpan(1.5f), 0, genres.length, 0)
            genres.setSpan(StyleSpan(Typeface.BOLD), 0, genres.length, 0)
            val genreListSpan = SpannableString("$genreListString\n\n")
            genreListSpan.setSpan(RelativeSizeSpan(1.5f), 0, genreListSpan.length, 0)
            val synopsis = SpannableString("\"" + jsonObject["overview"].toString() + "\"")
            synopsis.setSpan(RelativeSizeSpan(1.2f), 0, synopsis.length, 0)
            synopsis.setSpan(ForegroundColorSpan(Color.BLACK), 0, synopsis.length, 0)


            val fullText = SpannableString(TextUtils.concat(title, year, duration, durationValue, genres, genreListSpan, synopsis))
            description.setText(fullText, TextView.BufferType.SPANNABLE)


            setBackGround(jsonObject["backdrop_url"].toString(), jsonObject["poster_url"].toString())
        }else {
            Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Filme inválido", Snackbar.LENGTH_INDEFINITE).show()
        }
    }
}
