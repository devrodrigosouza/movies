package com.devrodrigo.movies

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.devrodrigo.movies.ui.theme.MoviesTheme
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndLoadCsv()
        setContent {
            MoviesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "movieList",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("movieList") {
                            val movies = rememberMoviesFromDb()
                            MovieList(movies = movies, navController = navController)
                        }
                        composable(
                            "movieDetail/{movieId}",
                            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                            MovieDetailScreen(movieId = movieId, db = db, navController = navController)
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun rememberMoviesFromDb(): List<ModelMovies> {
        val movieList = remember { mutableStateListOf<ModelMovies>() }

        LaunchedEffect(true) {
            lifecycleScope.launch(Dispatchers.IO) {
                val moviesFromDb = db.movieDao().getAll()
                movieList.clear()
                movieList.addAll(moviesFromDb)
            }
        }

        return movieList
    }



    private fun checkAndLoadCsv() {
        lifecycleScope.launch(Dispatchers.IO) {
            val moviesInDb = db.movieDao().getAll()
            if (moviesInDb.isEmpty()) {
                csvLoad()
            }
        }
    }

    private fun csvLoad() {
        val inputStream: InputStream = assets.open("TMDB.csv")
        val db = AppDatabase.getDatabase(applicationContext)

        lifecycleScope.launch(Dispatchers.IO) {
            val movieList = mutableListOf<ModelMovies>()

            csvReader().open(inputStream) {
                readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                    val movie = ModelMovies(
                        id = row["id"]?.toIntOrNull() ?: 0,
                        title = row["title"]?.takeIf { it.isNotBlank() } ?: "Unknown",
                        vote_average = row["vote_average"]?.toDoubleOrNull() ?: 0.0,
                        vote_count = row["vote_count"]?.toIntOrNull() ?: 0,
                        status = row["status"]?.takeIf { it.isNotBlank() } ?: "Unknown",
                        release_date = row["release_date"]?.takeIf { it.isNotBlank() } ?: "Unknown",
                        revenue = row["revenue"]?.toLongOrNull() ?: 0L,
                        runtime = row["runtime"]?.toIntOrNull() ?: 0,
                        adult = row["adult"]?.lowercase()?.toBooleanStrictOrNull() ?: false,
                        backdrop_path = row["backdrop_path"]?.takeIf { it.isNotBlank() } ?: "Unknown"
                    )

                    movieList.add(movie)


                    if (movieList.size >= 500) {
                        lifecycleScope.launch {
                            db.movieDao().insertMovies(movieList.toList())
                            movieList.clear()
                        }
                    }
                }

                if (movieList.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.movieDao().insertMovies(movieList)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "Dados carregados com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MoviesTheme {
        Greeting("Android")
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieList(
    movies: List<ModelMovies>,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = movies.size,
            key = { index -> movies[index].id }
        ) { index ->
            val movie = movies[index]
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = CardDefaults.shape,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("movieDetail/${movie.id}")
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = movie.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nota: ${movie.vote_average}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun MovieDetailScreen(movieId: Int, db: AppDatabase, navController: NavController) {
    val movieState = remember { mutableStateOf<ModelMovies?>(null) }
    val openDialog = remember { mutableStateOf(false) }

    LaunchedEffect(movieId) {
        val movie = db.movieDao().getMovieById(movieId)
        movieState.value = movie
        openDialog.value = true
    }

    if (openDialog.value) {
        movieState.value?.let { movie ->
            AlertDialog(
                onDismissRequest = {
                    openDialog.value = false
                    navController.popBackStack()
                },
                title = {
                    Text(text = movie.title)
                },
                text = {
                    Text(
                        text = "Status: ${movie.status}\n" +
                                "Lançamento: ${movie.release_date}\n" +
                                "Revenue: ${movie.revenue}",
                        modifier = Modifier.padding(8.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openDialog.value = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("Fechar")
                    }
                }
            )
        }
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text(text = "Carregando...", modifier = Modifier.padding(16.dp))
        }
    }
}


