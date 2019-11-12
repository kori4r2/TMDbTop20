package com.example.tmdbtop20

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.movie_entry_layout.view.*
import org.json.JSONObject
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

    class MovieClickListener(_context: Context, _cache: DiskBasedCache?, _queue: RequestQueue): View.OnClickListener{
        private val context: Context = _context
        private val cache: DiskBasedCache? = _cache
        private val queue: RequestQueue = _queue

        override fun onClick(v: View?) {
            if(((context as Activity).findViewById<ProgressBar>(R.id.progressBar)).visibility == View.VISIBLE){
                return
            }

            Log.println(Log.INFO, null, "Clicou no filme de id " + v?.tag.toString())
            val tag: Long = (v?.tag ?: "-1").toString().toLong()
            // Muda pra cena do filme la
            if(v != null && tag > -1) {
                val progressBar: ProgressBar = context.findViewById(R.id.progressBar)
                val overlay: ImageView = context.findViewById(R.id.overlay)
                progressBar.visibility = View.VISIBLE
                overlay.visibility = View.VISIBLE
                val movieUrl = "$apiUrl/$tag"

                val cacheEntry: Cache.Entry? = cache?.get(movieUrl)

                if(cacheEntry != null){
                    // Load json from cache
                    val response = JSONObject(String(cacheEntry.data))

                    val intent: Intent = Intent(context, MovieDetails::class.java).apply {
                        putExtra(MOVIE_INFO, response.toString())
                    }
                    context.startActivity(intent)

                    progressBar.visibility = View.INVISIBLE
                    overlay.visibility = View.INVISIBLE
                }else{
                    val jsonRequest = JsonObjectRequest(Request.Method.GET, movieUrl, null,
                        Response.Listener<JSONObject> { response ->
                            if(response != null) {
                                val intent: Intent = Intent(context, MovieDetails::class.java).apply {
                                    putExtra(MOVIE_INFO, response.toString())
                                }
                                context.startActivity(intent)

                                progressBar.visibility = View.INVISIBLE
                                overlay.visibility = View.INVISIBLE
                            }else{
                                progressBar.visibility = View.INVISIBLE
                                overlay.visibility = View.INVISIBLE
                                Snackbar.make(context.findViewById(R.id.myCoordinatorLayout),
                                                                                "Informação inválida",
                                                                                Snackbar.LENGTH_LONG).show()
                            }
                        },
                        Response.ErrorListener {
                            progressBar.visibility = View.INVISIBLE
                            overlay.visibility = View.INVISIBLE
                            Snackbar.make(context.findViewById(R.id.myCoordinatorLayout),
                                "Erro de conexão",
                                Snackbar.LENGTH_LONG).show()
                        })

                    queue.add(jsonRequest)
                }
            }
        }

    }
    val clickListener: MovieClickListener = MovieClickListener(context, cache, queue)

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
                return myView
            }catch (e: Exception){
                myView.movieThumbnail.setImageBitmap(errorThumbnail)
            }
        }

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

        return myView
    }

    fun stop(){
        queue.stop()
    }
}