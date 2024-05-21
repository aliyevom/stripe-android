package com.stripe.android.lpmfoundations.luxe

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.ui.core.elements.SharedDataSpec

internal data class SupportedPaymentMethod(
    /**
     * This describes the PaymentMethod Type as described
     * https://stripe.com/docs/api/payment_intents/create#create_payment_intent-payment_method_types
     */
    val code: PaymentMethodCode,

    /** This describes the name that appears under the selector. */
    val displayName: ResolvableString,

    /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
    @DrawableRes val iconResource: Int,

    /** An optional light theme icon url if it's supported. */
    val lightThemeIconUrl: String?,

    /** An optional dark theme icon url if it's supported. */
    val darkThemeIconUrl: String?,

    /** Indicates if the lpm icon in the selector requires tinting. */
    val iconRequiresTinting: Boolean,

    /** The subtitle, or marketing copy for an LPM. */
    val subtitle: ResolvableString? = null,
) {
    constructor(
        paymentMethodDefinition: PaymentMethodDefinition,
        sharedDataSpec: SharedDataSpec? = null,
        @StringRes displayNameResource: Int,
        @DrawableRes iconResource: Int,
        iconRequiresTinting: Boolean = false,
        subtitle: ResolvableString? = null,
    ) : this(
        code = paymentMethodDefinition.type.code,
        displayName = resolvableString(id = displayNameResource),
        iconResource = iconResource,
        lightThemeIconUrl = sharedDataSpec?.selectorIcon?.lightThemePng,
        darkThemeIconUrl = sharedDataSpec?.selectorIcon?.darkThemePng,
        iconRequiresTinting = iconRequiresTinting,
        subtitle = subtitle,
    )

    constructor(
        code: PaymentMethodCode,
        @StringRes displayNameResource: Int,
        @DrawableRes iconResource: Int,
        iconRequiresTinting: Boolean = false,
        lightThemeIconUrl: String?,
        darkThemeIconUrl: String?,
        subtitle: ResolvableString? = null,
    ) : this(
        code = code,
        displayName = resolvableString(id = displayNameResource),
        iconResource = iconResource,
        lightThemeIconUrl = lightThemeIconUrl,
        darkThemeIconUrl = darkThemeIconUrl,
        iconRequiresTinting = iconRequiresTinting,
        subtitle = subtitle,
    )
}
