package environment.adversarial.hex

import environment.EnvironmentType.EnvironmentType
import environment._

object HexEnvironment {
  def createRow(size: Int, y: Int, stateList: List[Int]): List[Cell] = {
    for {
      x <- (0 until size).toList
      index = (y * size) + x
      cellType = HexCellType(stateList(index))
    } yield {
      HexCell(x, y, cellType)
    }
  }

  def apply(stateList: List[Int]): HexEnvironment = {
    val size = Math.sqrt(stateList.size).toInt

    val grid = for {
      y <- (0 until size).toList
      row = createRow(size, y, stateList)
    } yield {
      row
    }

    val board = HexBoard(grid)

    HexEnvironment(board)
  }
}

case class HexEnvironment(board: HexBoard) extends Environment {
  val environmentType: EnvironmentType = EnvironmentType.Hex

//  val blueWins: Boolean = {
//    val firstColumn = board.grid.flatten.filter(_.xIndex == 0)
//    (for {
//      cell <- firstColumn
//      blueCell = cell if cell.cellType == HexCellType.Blue.id
//    } yield {
//      isWinningCell(blueCell)
//    }).contains(true)
//  }
//
//  val redWins: Boolean = {
//    val firstRow = board.grid.head
//    (for {
//      cell <- firstRow
//      redCell = cell if cell.cellType == HexCellType.Red.id
//    } yield {
//      isWinningCell(redCell)
//    }).contains(true)
//  }
  val redWins: Boolean = {
    val firstColumn = board.grid.flatten.filter(_.xIndex == 0)
    (for {
      cell <- firstColumn
      redCell = cell if cell.cellType == HexCellType.Red.id
    } yield {
      isWinningCell(redCell)
    }).contains(true)
  }

  val blueWins: Boolean = {
    val firstRow = board.grid.head
    (for {
      cell <- firstRow
      blueCell = cell if cell.cellType == HexCellType.Blue.id
    } yield {
      isWinningCell(blueCell)
    }).contains(true)
  }

  val reward: Double = {
    if (redWins || blueWins) 1.0
    else 0.0
  }

  override def isDone: Boolean = redWins || blueWins

  val possibleActions: List[Action] = {
    if (isDone) {
      List.empty
    } else {
      for {
        row <- board.grid
        cell <- row
        emptyCell = cell if cell.isEmpty
      } yield {
        HexAction(emptyCell.xIndex, emptyCell.yIndex, 0)
      }
    }
  }

  private def isWinningCell(cell: Cell, visitedCells: Set[Cell] = Set.empty): Boolean = {
    val neighboringCells = board.neighbors(cell: Cell)

    val newVisitedCells     = visitedCells ++ neighboringCells + cell
    val newNeighboringCells = neighboringCells -- visitedCells

    if (newNeighboringCells.isEmpty) {
      false
    } else if (neighboringCells.exists(neighboringCell =>
                 (neighboringCell.cellType == HexCellType.Blue.id && neighboringCell.yIndex == board.grid.size - 1) || (neighboringCell.cellType == HexCellType.Red.id && neighboringCell.xIndex == board.grid.head.size - 1))) {
      true
    } else if (newVisitedCells.size == visitedCells.size) {
      false
    } else {
      newNeighboringCells.exists(neighboringCell => isWinningCell(neighboringCell, newVisitedCells))
    }
  }

  def step(action: Action): Environment = {
    val newGrid = for {
      gridRow <- board.grid
      newGridRow = updateGridRowByAction(action, gridRow)
    } yield {
      newGridRow
    }

    val newBoard = HexBoard(newGrid)
    HexEnvironment(newBoard)
  }

  private def updateGridRowByAction(action: Action, row: List[Cell]): List[Cell] = {
    for {
      cell <- row
    } yield {
      if (cell.xIndex == action.xIndex && cell.yIndex == action.yIndex) {
        val cellType = if (board.redCellsCount == board.blueCellsCount) HexCellType.Red else HexCellType.Blue
        HexCell(cell.xIndex, cell.yIndex, cellType)
      } else {
        cell
      }
    }
  }

  def toggleCell(x: Int, y: Int): Environment = {
    val newGrid = for {
      gridRow <- board.grid
      newGridRow = updateGridRowByToggle(x, y, gridRow)
    } yield {
      newGridRow
    }

    val newBoard = HexBoard(newGrid)
    HexEnvironment(newBoard)
  }

  private def updateGridRowByToggle(x: Int, y: Int, row: List[Cell]): List[Cell] = {
    for {
      cell <- row
      cellStartX = cell.xIndex * board.cellWidth
      cellStartY = cell.yIndex * board.cellHeight
    } yield {
      if (x > cellStartX && x < cellStartX + board.cellWidth && y > cellStartY && y < cellStartY + board.cellHeight) {
        HexCellType(cell.cellType) match {
          case HexCellType.Red   => HexCell(cell.xIndex, cell.yIndex, HexCellType.Blue)
          case HexCellType.Blue  => HexCell(cell.xIndex, cell.yIndex, HexCellType.Empty)
          case HexCellType.Empty => HexCell(cell.xIndex, cell.yIndex, HexCellType.Red)
          case _                 => cell
        }
      } else {
        cell
      }
    }
  }

  override def toString: String = {
    board.grid.map(_.map(_.toString).mkString("")).mkString("\n")
  }
}
