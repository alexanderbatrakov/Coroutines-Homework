package otus.homework.reactivecats

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import otus.homework.coroutines.CatModels
import otus.homework.coroutines.CatsService
import otus.homework.coroutines.CrashMonitor

class CatsViewModel(
    private val catsService: CatsService,
) : ViewModel() {

    private val _state = MutableLiveData<CatsResult>()
    val state: LiveData<CatsResult> = _state

    private val errorsHandler = CoroutineExceptionHandler { _, throwable ->
        CrashMonitor.trackWarning()
        _state.postValue(CatsResult.Errors(throwable))
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(errorsHandler) {
            val getCatFactDiffered = async { catsService.getCatFact() }
            val getCatImageDiffered = async { catsService.getCatImage() }

            val getCatFactResponse = getCatFactDiffered.await()
            val getCatImageResponse = getCatImageDiffered.await().firstOrNull()

            val catModelsMapper = CatModels(
                fact = getCatFactResponse.fact,
                url = getCatImageResponse?.url.orEmpty(),
                width = getCatImageResponse?.width ?: 0,//значение не использую, но пусть будет
                height = getCatImageResponse?.height ?: 0,//значение не использую, но пусть будет
            )
            _state.value = CatsResult.Success(catModelsMapper)
        }
    }

    sealed class CatsResult {
        data class Success(val catModels: CatModels) : CatsResult()
        data class Errors(val e: Throwable) : CatsResult()
    }
}