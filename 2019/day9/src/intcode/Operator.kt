package intcode

import java.math.BigDecimal

const val LOCATION_MODE = 0
const val POSITION_MODE = 1
const val RELATIVE_MODE = 2

/**
 * Horrible
 */
object RelativeBase{
    var value = 0

    fun reset() {
        value = 0
    }
}

interface Operator {
    val opCode: Int
    val numOperands: Int
    val isTerminated: Boolean
    val isJump: Boolean
    val requiresInput: Boolean

    val reader: (index: Int) -> BigDecimal
    val writer: (index: Int, value: BigDecimal) -> Unit

    fun operate(operands: List<BigDecimal>): BigDecimal?

    fun getMode(opNum: Int): Int {
        fun getDecimalPlace(from: Int, place: Int): Int {
            /* WOT NO **  agghhh */
            var divider = 1
            var moder = 1
            for (i in 1 until place)
                divider *= 10
            for (i in 1..place)
                moder *= 10

            return from % moder / divider
        }

        // The first opNum is described at the third decimal place
        return getDecimalPlace(this.opCode, opNum + 2)
    }

    fun getValue(operand: BigDecimal, operandPos:Int, relativeBase:Int=0): BigDecimal =
        when(getMode(operandPos)) {
            LOCATION_MODE -> this.reader(operand.toInt())
            POSITION_MODE -> operand
            RELATIVE_MODE -> this.reader(operand.toInt() + RelativeBase.value)
            else -> throw IllegalStateException("Unknown mode: ${getMode(operandPos)}")
        }

    companion object {
        fun create(
            opCode: Int,
            reader: (index: Int) -> BigDecimal,
            writer: (index: Int, value: BigDecimal) -> Unit
        ): Operator {
            return when(opCode % 100) {
                1 -> AddOperator(opCode, reader, writer)
                2 -> MultiplyOperator(opCode, reader, writer)
                3 -> InputOperator(opCode, reader, writer)
                4 -> OutputOperator(opCode, reader, writer)
                5 -> JumpIfTrueOperator(opCode, reader, writer)
                6 -> JumpIfFalseOperator(opCode, reader, writer)
                7 -> LessThanOperator(opCode, reader, writer)
                8 -> EqualsOperator(opCode, reader, writer)
                9 -> AdjustRelativeBaseOperator(opCode, reader, writer)
                99 -> TerminationOperator(opCode, reader, writer)
                else -> throw IllegalStateException("Unknown opcode $opCode")
            }
        }
    }
}

data class NoOpOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 0
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = false
    override fun operate(operands: List<BigDecimal>): BigDecimal? = null
}

data class TerminationOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 0
    override val isTerminated = true
    override val isJump = false
    override val requiresInput = false
    override fun operate(operands: List<BigDecimal>): BigDecimal? = null
}

data class AddOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 3
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? {
        val output = this.getValue(operands[0], 1) + this.getValue(operands[1], 2)
        this.writer(operands[2].toInt(), output)
        return null
    }
}

data class MultiplyOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value: BigDecimal) -> Unit
) : Operator {
    override val numOperands = 3
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? {
        val output = this.getValue(operands[0], 1) * this.getValue(operands[1], 2)
        this.writer(operands[2].toInt(), output)
        return null
    }
}

data class InputOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 1
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = true

    override fun operate(operands: List<BigDecimal>): BigDecimal? {
        println("Input Operator: $operands")
        this.writer(operands[0].toInt(), this.getValue(operands[1], 2))
        //this.writer(operands[0].toInt(), operands[1])
        return null
    }
}

data class OutputOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 1
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? {
        //println("Output Operator")
        return this.getValue(operands[0], 1)
    }
}

data class JumpIfTrueOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 2
    override val isTerminated = false
    override val isJump = true
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? =
        if(this.getValue(operands[0], 1) != BigDecimal(0))
            this.getValue(operands[1], 2)
        else
            null
}

data class JumpIfFalseOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 2
    override val isTerminated = false
    override val isJump = true
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? =
        if(this.getValue(operands[0], 1) == BigDecimal(0))
            this.getValue(operands[1], 2)
        else
            null
}

data class LessThanOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 3
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? {
        if(this.getValue(operands[0], 1) < this.getValue(operands[1], 2))
            this.writer(operands[2].toInt(), BigDecimal(1))
        else
            this.writer(operands[2].toInt(), BigDecimal(0))

        return null
    }
}

data class EqualsOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 3
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? {
        if(this.getValue(operands[0], 1) == this.getValue(operands[1], 2))
            this.writer(operands[2].toInt(), BigDecimal(1))
        else
            this.writer(operands[2].toInt(), BigDecimal(0))

        return null
    }
}

data class AdjustRelativeBaseOperator(
    override val opCode: Int,
    override val reader: (index: Int) -> BigDecimal,
    override val writer: (index: Int, value:BigDecimal) -> Unit
) : Operator {
    override val numOperands = 1
    override val isTerminated = false
    override val isJump = false
    override val requiresInput = false

    override fun operate(operands: List<BigDecimal>): BigDecimal? {
        RelativeBase.value += this.getValue(operands[0], 1).toInt()
        println("Relative base: ${RelativeBase.value}")
        //RelativeBase.value += operands[0].toInt()
        return null
    }
}