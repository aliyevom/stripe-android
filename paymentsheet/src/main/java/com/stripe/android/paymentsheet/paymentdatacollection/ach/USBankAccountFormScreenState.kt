package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.os.Parcelable
import androidx.annotation.StringRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.financialconnections.model.BankAccount
import kotlinx.parcelize.Parcelize

internal sealed class USBankAccountFormScreenState(
    @StringRes open val error: Int? = null,
    open val isProcessing: Boolean = false
) : Parcelable {
    abstract val primaryButtonText: ResolvableString
    abstract val mandateText: String?

    @Parcelize
    data class BillingDetailsCollection(
        @StringRes override val error: Int? = null,
        override val primaryButtonText: ResolvableString,
        override val isProcessing: Boolean,
    ) : USBankAccountFormScreenState() {

        override val mandateText: String?
            get() = null
    }

    @Parcelize
    data class MandateCollection(
        val resultIdentifier: ResultIdentifier,
        val bankName: String?,
        val last4: String?,
        val intentId: String?,
        override val primaryButtonText: ResolvableString,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class VerifyWithMicrodeposits(
        val paymentAccount: BankAccount,
        val financialConnectionsSessionId: String,
        val intentId: String?,
        override val primaryButtonText: ResolvableString,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()

    @Parcelize
    data class SavedAccount(
        val financialConnectionsSessionId: String?,
        val intentId: String?,
        val bankName: String,
        val last4: String?,
        override val primaryButtonText: ResolvableString,
        override val mandateText: String?,
    ) : USBankAccountFormScreenState()

    internal sealed interface ResultIdentifier : Parcelable {
        @Parcelize
        data class Session(val id: String) : ResultIdentifier

        @Parcelize
        data class PaymentMethod(val id: String) : ResultIdentifier
    }
}
