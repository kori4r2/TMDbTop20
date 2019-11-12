package com.example.tmdbtop20

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import kotlinx.android.synthetic.main.movie_entry_layout.view.*
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.ByteArrayInputStream

class MovieDetails : AppCompatActivity() {

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
            val jsonObject: JSONObject = JSONObject(intentString)
            errorThumbnail = BitmapFactory.decodeResource(resources, R.drawable.error_thumbnail)

            cache = DiskBasedCache(cacheDir, cacheSize)
            // Configurar descrição do filme
            val description: TextView = findViewById(R.id.description)
            description.text = jsonObject["overview"].toString()
            setBackGround(jsonObject["backdrop_url"].toString(), jsonObject["poster_url"].toString())
        }else {
            Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Filme inválido", Snackbar.LENGTH_INDEFINITE).show()
        }
    }
}
