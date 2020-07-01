package ke.tang.refresh.sample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val data = MutableLiveData<Boolean>()

    /**
     * Simulate network request
     */
    fun requestData() {
        viewModelScope.launch {
            delay(3000)
            data.value = true
        }
    }
}