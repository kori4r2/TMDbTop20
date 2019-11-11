package com.example.tmdbtop20

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val apiUrl: String = "https://desafio-mobile.nyc3.digitaloceanspaces.com/movies"
    private var cache: DiskBasedCache? = null

    class MovieEntry(
        _id: Long?,
        _vote: Double?,
        _title: String?,
        _poster: String?,
        _genres: Array<String>?,
        _release: Date?
    ) {
        val id: Long? = _id
        val vote_average: Double? = _vote
        val title: String? = _title
        val poster_url: String? = _poster
        val genres: Array<String>? = _genres
        val release_date: Date? = _release
    }

    var currentEntries: MutableList<MovieEntry> = mutableListOf<MovieEntry>()
    var adapter: MovieEntryAdapter? = null

    private fun parseResponse(response: JSONArray) {
        if (response.length() > 0) {
            clearMovies()
            for (i in 0 until response.length()) {
                val obj = response.getJSONObject(i)
                val id: Long? = obj.getLong("id")
                val score: Double? = obj.getDouble("vote_average")
                val title: String? = obj.getString("title")
                val poster: String? = obj.getString("poster_url")
                val genreList = obj.getJSONArray("genres")
                val genres: Array<String>? = Array(genreList.length()) { genreList.getString(it) }
                val date: Date? = formatter.parse(obj.getString("release_date"))

                addMovie(MovieEntry(id, score, title, poster, genres, date))
            }
        }
    }

    private fun searchForNewMovies() {

        val cacheEntry: Cache.Entry? = cache?.get(apiUrl) ?: null
        if (cacheEntry != null) {
            Log.println(Log.INFO, null, "Found info in cache")
            val response = JSONArray(String(cacheEntry.data))

            parseResponse(response)

            swipeLayout.isRefreshing = false
        } else {

            val network = BasicNetwork(HurlStack())
            val queue: RequestQueue = RequestQueue(cache, network).apply {
                start()
            }

            val jsonArrayRequest = JsonArrayRequest(apiUrl,
                Response.Listener<JSONArray> { response: JSONArray? ->
                    if (response != null) {
                        parseResponse(response)
                    }
                    printMovies()
                    queue.stop()
                    swipeLayout.isRefreshing = false
                },
                Response.ErrorListener { error: VolleyError? ->
                    queue.stop()
                    swipeLayout.isRefreshing = false
                    Snackbar.make(findViewById(R.id.myCoordinatorLayout),
                                                R.string.connectionError,
                                                Snackbar.LENGTH_LONG).show()
                })

            queue.add(jsonArrayRequest)
        }
    }

    private fun printMovies() {
        for(i in 0 until currentEntries.size){
            Log.println(Log.INFO, null, currentEntries[i].title)
        }
    }

    private fun clearMovies(){
        adapter?.clear()
    }

    private fun addMovie(newMovie: MovieEntry){
        if(currentEntries.find { it.id == newMovie.id } == null){
            adapter?.add(newMovie)
        }
    }

    private fun removeMovie(newMovie: MovieEntry){
        if(currentEntries.find { it.id == newMovie.id } != null){
            adapter?.remove(newMovie)
        }
    }

    override fun onRefresh() {
        searchForNewMovies()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cache = DiskBasedCache(cacheDir, 1024 * 1024)

        adapter = MovieEntryAdapter(this, currentEntries, cache)
        movieList.adapter = adapter;
        swipeLayout.isRefreshing = true
        searchForNewMovies()

        swipeLayout.setOnRefreshListener{
            onRefresh()
        }
    }


    override fun onStop() {
        super.onStop()
        adapter?.stop()
    }
}
