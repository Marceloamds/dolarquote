package br.com.dollar.presentation.util.error

import br.com.dollar.domain.entity.error.ErrorData
import br.com.dollar.domain.entity.error.HttpErrorType
import br.com.dollar.domain.entity.error.RequestException
import br.com.dollar.domain.util.resource.Strings
import br.com.dollar.presentation.util.dialog.DialogData

class ErrorHandler constructor(
    private val strings: Strings
) {

    fun getDialogData(
        throwable: Throwable,
        retryAction: (() -> Unit)?
    ): DialogData {
        val data = getErrorData(throwable, retryAction)
        return data.tryAgainAction?.let {
            DialogData.error(strings, data.message, strings.globalTryAgain, it)
        } ?: DialogData.error(strings, data.message, strings.globalOk)
    }

    private fun getErrorData(
        throwable: Throwable,
        tryAgainAction: (() -> Unit)? = null
    ): ErrorData {
        return if (throwable is RequestException) {
            handleRequestException(throwable, tryAgainAction)
        } else {
            ErrorData.UnexpectedErrorData(strings.errorUnknown, tryAgainAction)
        }
    }

    private fun handleRequestException(
        exception: RequestException,
        tryAgainAction: (() -> Unit)? = null
    ): ErrorData {
        return when (exception) {
            is RequestException.TimeoutError ->
                ErrorData.TimeOutErrorData(strings.errorSocketTimeout, tryAgainAction)
            is RequestException.NetworkError -> ErrorData.NetworkErrorData(
                strings.errorNetwork,
                tryAgainAction
            )
            is RequestException.UnexpectedError -> ErrorData.UnexpectedErrorData(
                exception.errorMessage ?: strings.errorUnknown,
                tryAgainAction
            )
            is RequestException.HttpError -> resolveHttpError(exception, tryAgainAction)
        }
    }

    private fun resolveHttpError(
        exception: RequestException,
        tryAgainAction: (() -> Unit)?
    ): ErrorData {
        return when (exception.httpErrorType) {
            HttpErrorType.NOT_FOUND -> ErrorData.ClientEndHttpErrorData(
                exception.errorMessage ?: strings.errorNotFound
            )
            HttpErrorType.TIMEOUT -> ErrorData.HttpErrorData(
                strings.errorSocketTimeout,
                tryAgainAction
            )
            HttpErrorType.INTERNAL_SERVER_ERROR -> ErrorData.HttpErrorData(
                strings.errorInternalServer,
                tryAgainAction
            )
            HttpErrorType.UNEXPECTED_ERROR -> ErrorData.HttpErrorData(
                strings.errorUnexpected,
                tryAgainAction
            )
            HttpErrorType.BAD_REQUEST -> ErrorData.ClientEndHttpErrorData(strings.errorBadRequest)
            HttpErrorType.CONFLICT -> ErrorData.ClientEndHttpErrorData(strings.errorConflict)
            HttpErrorType.FORBIDDEN -> ErrorData.ClientEndHttpErrorData(strings.errorForbidden)
            HttpErrorType.UNAUTHORIZED -> ErrorData.ClientEndHttpErrorData(strings.errorUnauthorized)
            HttpErrorType.UN_PROCESSABLE_ENTITY -> ErrorData.ClientEndHttpErrorData(strings.errorUnprocessableEntity)
            else -> ErrorData.HttpErrorData(
                exception.errorMessage
                    ?: exception.message
                    ?: strings.errorUnknown,
                tryAgainAction
            )
        }
    }
}