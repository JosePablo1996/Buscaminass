package com.example.buscaminas

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gridView: GridView
    private lateinit var minesCount: TextView
    private lateinit var timerText: TextView
    private lateinit var resetButton: Button

    private val gridSize = 8
    private val totalMines = 10
    private lateinit var cells: Array<Array<Cell>>
    private lateinit var adapter: MyCellAdapter
    private var gameOver = false
    private var timeElapsed = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeGame()
        setupTimer()
    }

    private fun initializeViews() {
        gridView = findViewById(R.id.gridView)
        minesCount = findViewById(R.id.minesCount)
        timerText = findViewById(R.id.timerText)
        resetButton = findViewById(R.id.resetButton)

        resetButton.setOnClickListener {
            resetGame()
        }
    }

    private fun initializeGame() {
        cells = Array(gridSize) { Array(gridSize) { Cell() } }
        placeMines()
        calculateAdjacentMines()

        adapter = MyCellAdapter(this, cells, this)
        gridView.adapter = adapter

        minesCount.text = "Minas: $totalMines"
    }

    private fun placeMines() {
        var minesPlaced = 0
        val random = kotlin.random.Random(System.currentTimeMillis())

        while (minesPlaced < totalMines) {
            val row = random.nextInt(gridSize)
            val col = random.nextInt(gridSize)

            if (!cells[row][col].isMine) {
                cells[row][col] = cells[row][col].copy(isMine = true)
                minesPlaced++
            }
        }
    }

    private fun calculateAdjacentMines() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (!cells[i][j].isMine) {
                    val count = countAdjacentMines(i, j)
                    cells[i][j] = cells[i][j].copy(adjacentMines = count)
                }
            }
        }
    }

    private fun countAdjacentMines(row: Int, col: Int): Int {
        var count = 0
        for (i in -1..1) {
            for (j in -1..1) {
                if (i == 0 && j == 0) continue

                val newRow = row + i
                val newCol = col + j

                if (newRow in 0 until gridSize && newCol in 0 until gridSize &&
                    cells[newRow][newCol].isMine) {
                    count++
                }
            }
        }
        return count
    }

    private fun setupTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (!gameOver) {
                    timeElapsed++
                    timerText.text = "Tiempo: $timeElapsed"
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    fun revealCell(row: Int, col: Int) {
        if (gameOver || cells[row][col].isRevealed) return

        cells[row][col] = cells[row][col].copy(isRevealed = true)

        if (cells[row][col].isMine) {
            gameOver = true
            revealAllMines()
            Toast.makeText(this, "¡Game Over! Has pisado una mina", Toast.LENGTH_LONG).show()
        } else if (cells[row][col].adjacentMines == 0) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i == 0 && j == 0) continue

                    val newRow = row + i
                    val newCol = col + j
                    if (newRow in 0 until gridSize && newCol in 0 until gridSize) {
                        revealCell(newRow, newCol)
                    }
                }
            }
        }

        checkWinCondition()
        adapter.notifyDataSetChanged()
    }

    private fun revealAllMines() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (cells[i][j].isMine) {
                    cells[i][j] = cells[i][j].copy(isRevealed = true)
                }
            }
        }
    }

    private fun checkWinCondition() {
        var unrevealedSafeCells = 0
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                if (!cells[i][j].isRevealed && !cells[i][j].isMine) {
                    unrevealedSafeCells++
                }
            }
        }

        if (unrevealedSafeCells == 0) {
            gameOver = true
            Toast.makeText(this, "¡Felicidades! Has ganado", Toast.LENGTH_LONG).show()
        }
    }

    private fun resetGame() {
        gameOver = false
        timeElapsed = 0
        timerText.text = "Tiempo: 0"
        initializeGame()
        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    override fun onPause() {
        super.onPause()
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (!gameOver) {
            timerHandler.postDelayed(timerRunnable, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }

    inner class MyCellAdapter(
        private val context: Context,
        private val cells: Array<Array<Cell>>,
        private val mainActivity: MainActivity
    ) : BaseAdapter() {

        private val gridSize = cells.size

        override fun getCount(): Int = gridSize * gridSize

        override fun getItem(position: Int): Cell {
            val row = position / gridSize
            val col = position % gridSize
            return cells[row][col]
        }

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.cell_layout, parent, false)

            val row = position / gridSize
            val col = position % gridSize
            val cell = cells[row][col]
            val cellButton = view.findViewById<Button>(R.id.cellButton)

            if (cell.isRevealed) {
                if (cell.isMine) {
                    cellButton.text = "💣"
                    cellButton.setBackgroundColor(0xFFFF0000.toInt())
                } else {
                    val adjacent = cell.adjacentMines
                    if (adjacent > 0) {
                        cellButton.text = adjacent.toString()
                        setNumberColor(cellButton, adjacent)
                    } else {
                        cellButton.text = ""
                    }
                    cellButton.setBackgroundColor(0xFFFFFFFF.toInt())
                }
            } else {
                cellButton.text = if (cell.isFlagged) "🚩" else ""
                cellButton.setBackgroundResource(R.drawable.cell_background)
            }

            cellButton.setOnClickListener {
                mainActivity.revealCell(row, col)
            }

            cellButton.setOnLongClickListener {
                if (!cell.isRevealed) {
                    cell.isFlagged = !cell.isFlagged
                    notifyDataSetChanged()
                }
                true
            }

            return view
        }

        private fun setNumberColor(button: Button, number: Int) {
            val color = when (number) {
                1 -> 0xFF0000FF.toInt()
                2 -> 0xFF008000.toInt()
                3 -> 0xFFFF0000.toInt()
                4 -> 0xFF000080.toInt()
                5 -> 0xFF800000.toInt()
                6 -> 0xFF008080.toInt()
                7 -> 0xFF000000.toInt()
                8 -> 0xFF808080.toInt()
                else -> 0xFF000000.toInt()
            }
            button.setTextColor(color)
        }
    }
}

data class Cell(
    var isMine: Boolean = false,
    var isRevealed: Boolean = false,
    var isFlagged: Boolean = false,
    var adjacentMines: Int = 0
)