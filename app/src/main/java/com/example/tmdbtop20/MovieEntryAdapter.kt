package com.example.tmdbtop20

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.android.volley.Cache
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.ImageRequest
import kotlinx.android.synthetic.main.movie_entry_layout.view.*
import java.io.ByteArrayInputStream
import kotlin.Exception

class MovieEntryAdapter(_context: Context, list: MutableList<MainActivity.MovieEntry>, _cache: DiskBasedCache?)
    : ArrayAdapter<MainActivity.MovieEntry>(_context,0, list){
    private val cache = _cache
    private val errorThumbnail = BitmapFactory.decodeResource(context.resources, R.drawable.error_thumbnail)
    private val network: BasicNetwork = BasicNetwork(HurlStack())
    private val queue: RequestQueue = RequestQueue(cache, network).apply {
        start()
    }

    class MovieClickListener: View.OnClickListener{
        override fun onClick(v: View?) {
            Log.println(Log.INFO, null, "Clicou no filme de id " + v?.tag.toString())
            // Muda pra cena do filme la
        }

    }
    val clickListener: MovieClickListener = MovieClickListener()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var thisEntry: MainActivity.MovieEntry? = getItem(position)
        var myView: View

        if(convertView == null) {
            myView = LayoutInflater.from(context).inflate(R.layout.movie_entry_layout, parent, false)
            myView.movieThumbnail.setOnClickListener(clickListener)
            myView.movieTitle.setOnClickListener(clickListener)
        }else{
            myView = convertView
        }
        myView.movieTitle.text = (thisEntry?.title ?: "Untitled")
        myView.movieThumbnail.setImageBitmap(errorThumbnail)

        val tagValue: Long = thisEntry?.id ?: -1
        myView.movieTitle.tag = tagValue
        myView.movieThumbnail.tag = tagValue
        val posterUrl: String = thisEntry?.poster_url ?: ""

        val cacheEntry: Cache.Entry? = cache?.get(posterUrl)
        if(cacheEntry != null){
            // Load image from cache
            val inputStream = ByteArrayInputStream(cacheEntry.data)
            try {
                val thumbnail = BitmapFactory.decodeStream(inputStream)
                myView.movieThumbnail.setImageBitmap(thumbnail)
            }catch (e: Exception){
                myView.movieThumbnail.setImageBitmap(errorThumbnail)
            }
        }else{
            val imageRequest = ImageRequest(posterUrl,
                Response.Listener<Bitmap> { response: Bitmap? ->
                    if(response != null) {
                        myView.movieThumbnail.setImageBitmap(response)
                    }else{
                        myView.movieThumbnail.setImageBitmap(errorThumbnail)
                    }
                }, 0, 0, ImageView.ScaleType.FIT_XY, null,
                Response.ErrorListener { error: VolleyError? ->
                    myView.movieThumbnail.setImageBitmap(errorThumbnail)
                })

            queue.add(imageRequest)
        }

        return myView
    }

    fun stop(){
        queue.stop()
    }
}