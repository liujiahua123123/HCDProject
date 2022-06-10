


abstract class Operation<I,O>(){

    open infix fun <N> then(another: Operation<O, N>): CombinedOperation<I,N>{
        return CombinedOperation.ofSingle(this) then another
    }

    abstract operator fun invoke(input: I):O
}


class CombinedOperation<I,O>:Operation<I,O>(){
    private val list: MutableList<Operation<*,*>> = mutableListOf()
    val size: Int
        get() = list.size


    companion object{
        fun <I,O> ofSingle(operation: Operation<I,O>): CombinedOperation<I,O>{
            return CombinedOperation<I, O>().also {
                it.list.add(operation)
            }
        }
    }

    override infix fun <N> then(another: Operation<O, N>): CombinedOperation<I, N> {
        return CombinedOperation<I, N>().also {
            it.list.addAll(list)
            it.list.add(another)
        }
    }

    override fun invoke(input: I): O {
        TODO("Not yet implemented")
    }

}