package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock

@Suppress("LargeClass")
class DefaultPaymentMethodVerticalLayoutInteractorTest {
    @Test
    fun state_updatesWhenProcessingUpdates() = runScenario {
        interactor.state.test {
            awaitItem().run {
                assertThat(isProcessing).isFalse()
            }
            processingSource.value = true
            awaitItem().run {
                assertThat(isProcessing).isTrue()
            }
        }
    }

    @Test
    fun state_updatesWhenSelectionUpdates() {
        val paymentMethodTypes = listOf("card", "cashapp")
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes
                )
            ),
            updateSelection = {},
            formElementsForCode = { emptyList() },
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayablePaymentMethods).isNotEmpty()
                    assertThat(selection).isNull()
                }
            }
            selectionSource.value = PaymentSelection.New.GenericPaymentMethod(
                labelResource = "CashApp",
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            )

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayablePaymentMethods).isNotEmpty()
                    assertThat(selection?.code()).isEqualTo("cashapp")
                }
            }
        }
    }

    @Test
    fun state_returnsCorrectSelectionForSavedPM() = runScenario(updateSelection = {}) {
        val savedSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )
        selectionSource.value = savedSelection

        dispatcher.scheduler.advanceUntilIdle()

        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods).isNotEmpty()
                assertThat(selection).isEqualTo(savedSelection)
            }
        }
    }

    @Test
    fun `state has manage_all saved payment method action when multiple saved PMs are available`() {
        runScenario(
            initialPaymentMethods = PaymentMethodFixtures.createCards(3),
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL
                    )
                }
            }
        }
    }

    @Test
    fun `state has manage_one saved PM action when one modifiable saved PM and allowsRemovalOfLast is true`() {
        runScenario(
            initialPaymentMethods = listOf(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD),
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE
                    )
                }
            }
        }
    }

    @Test
    fun `state has manage_one saved payment method action when one saved PM and allowsRemovalOfLast is true`() {
        runScenario(
            initialPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE
                    )
                }
            }
        }
    }

    @Test
    fun `state has edit card brand saved payment method action when one saved PM and allowsRemovalOfLast is false`() {
        runScenario(
            initialPaymentMethods = listOf(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD),
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.EDIT_CARD_BRAND
                    )
                }
            }
        }
    }

    @Test
    fun `state has no saved payment method action when one unmodifiable saved PM and allowsRemovalOfLast is false`() {
        runScenario(
            initialPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(availableSavedPaymentMethodAction).isEqualTo(
                        PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
                    )
                }
            }
        }
    }

    @Test
    fun `state displays most recently selected PM if it exists`() {
        val displayedPM = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(
            initialPaymentMethods = PaymentMethodFixtures.createCards(3).plus(displayedPM),
            initialMostRecentlySelectedSavedPaymentMethod = displayedPM,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }
        }
    }

    @Test
    fun `state displays first saved PM if it exists`() {
        val displayedPM = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(
            initialPaymentMethods = listOf(displayedPM).plus(PaymentMethodFixtures.createCards(3)),
            initialMostRecentlySelectedSavedPaymentMethod = null,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }
        }
    }

    @Test
    fun `state updates displayed saved PM if it changes`() {
        val displayedPM = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentMethods = listOf(displayedPM).plus(PaymentMethodFixtures.createCards(3))
        runScenario(
            initialPaymentMethods = paymentMethods,
            initialMostRecentlySelectedSavedPaymentMethod = displayedPM,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPM)
                }
            }

            val newDisplayedSavedPm = paymentMethods[2]
            mostRecentlySelectedSavedPaymentMethodSource.value = newDisplayedSavedPm

            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(newDisplayedSavedPm)
                }
            }
        }
    }

    @Test
    fun `calling state_displayablePaymentMethods_onClick calls ViewAction_PaymentMethodSelected`() {
        var onFormFieldValuesChangedCalled = false
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            formElementsForCode = {
                listOf()
            },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEmpty()
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            }
        ) {
            val paymentMethod = interactor.state.value.displayablePaymentMethods.first { it.code == "cashapp" }
            paymentMethod.onClick()
            assertThat(onFormFieldValuesChangedCalled).isTrue()
        }
    }

    @Test
    fun noSavedPaymentMethods_noDisplayedSavedPaymentMethod() = runScenario(
        initialPaymentMethods = emptyList(),
        initialMostRecentlySelectedSavedPaymentMethod = null,
    ) {
        interactor.state.test {
            awaitItem().run {
                assertThat(displayedSavedPaymentMethod).isNull()
            }
        }
    }

    @Test
    fun stateDoesNotReturnWalletPaymentMethodsWhenInPaymentSheet() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        isFlowController = false,
    ) {
        walletsState.value = WalletsState(
            link = WalletsState.Link("email@email.com"),
            googlePay = WalletsState.GooglePay(
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = true,
                billingAddressParameters = null,
            ),
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code }).containsNoneOf("link", "google_pay")
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun stateIncludesWalletPaymentMethodsWhenInFlowController() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        isFlowController = true,
    ) {
        walletsState.value = WalletsState(
            link = WalletsState.Link("email@email.com"),
            googlePay = WalletsState.GooglePay(
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = true,
                billingAddressParameters = null,
            ),
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code })
                    .isEqualTo(listOf("card", "link", "google_pay", "cashapp"))
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun walletDisplayablePaymentMethodsUpdateWalletSelection() {
        var selectedWallet: PaymentSelection? = null
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp")
                )
            ),
            isFlowController = true,
            updateSelection = { selectedWallet = it },
        ) {
            walletsState.value = WalletsState(
                link = WalletsState.Link("email@email.com"),
                googlePay = WalletsState.GooglePay(
                    buttonType = GooglePayButtonType.Pay,
                    allowCreditCards = true,
                    billingAddressParameters = null,
                ),
                buttonsEnabled = true,
                dividerTextResource = 0,
                onGooglePayPressed = {},
                onLinkPressed = {},
            )
            assertThat(selectedWallet).isNull()

            val displayablePaymentMethods = interactor.state.value.displayablePaymentMethods
            displayablePaymentMethods.first { it.code == "link" }.onClick()
            assertThat(selectedWallet).isEqualTo(PaymentSelection.Link)
            displayablePaymentMethods.first { it.code == "google_pay" }.onClick()
            assertThat(selectedWallet).isEqualTo(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun stateDoesNotReturnWalletPaymentMethodsWhenInFlowControllerAndGooglePayIsNotAvailable() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        isFlowController = true,
    ) {
        walletsState.value = WalletsState(
            link = WalletsState.Link("email@email.com"),
            googlePay = null,
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code })
                    .isEqualTo(listOf("card", "cashapp"))
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun stateIncludesGooglePayWhenInFlowControllerAndLinkIsNotAvailable() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        ),
        isFlowController = true,
    ) {
        walletsState.value = WalletsState(
            link = null,
            googlePay = WalletsState.GooglePay(
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = true,
                billingAddressParameters = null,
            ),
            buttonsEnabled = true,
            dividerTextResource = 0,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
        interactor.state.test {
            awaitItem().run {
                assertThat(displayablePaymentMethods.map { it.code })
                    .isEqualTo(listOf("card", "google_pay", "cashapp"))
            }
        }
        interactor.showsWalletsHeader.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenFieldsAllowUserInteraction() {
        var calledFormScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            formElementsForCode = {
                formFieldsWhichRequireUserInteraction
            },
            formScreenFactory = {
                calledFormScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("card"))
            assertThat(calledFormScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenSelectedIsUsBank() {
        var calledFormScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            formElementsForCode = {
                listOf()
            },
            formScreenFactory = {
                calledFormScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("us_bank_account"))
            assertThat(calledFormScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenSelectedIsInstantDebits() {
        var calledFormScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            formElementsForCode = {
                listOf()
            },
            formScreenFactory = {
                calledFormScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("link"))
            assertThat(calledFormScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_updatesSelectedLPM() {
        var onFormFieldValuesChangedCalled = false
        runScenario(
            formElementsForCode = {
                listOf()
            },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEmpty()
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
            assertThat(onFormFieldValuesChangedCalled).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_updatesSelectedLPMAndMandate() {
        var onFormFieldValuesChangedCalled = false
        var mostRecentMandate: ResolvableString? = null
        val paymentMethodTypes = listOf("card", "cashapp")
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes
            )
        )
        runScenario(
            paymentMethodMetadata = paymentMethodMetadata,
            formElementsForCode = {
                val uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                    cardAccountRangeRepositoryFactory = mock(),
                    paymentMethodCreateParams = null,
                    paymentMethodExtraParams = null,
                )
                paymentMethodMetadata.formElementsForCode(it, uiDefinitionFactoryArgumentsFactory)!!
            },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEmpty()
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            },
            onMandateTextUpdated = { mostRecentMandate = it },
        ) {
            assertThat(mostRecentMandate).isNull()
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
            assertThat(onFormFieldValuesChangedCalled).isTrue()
            assertThat(mostRecentMandate).isEqualTo(
                resolvableString(
                    R.string.stripe_cash_app_pay_mandate,
                    paymentMethodMetadata.merchantName,
                    paymentMethodMetadata.merchantName
                )
            )
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_sameSavedPaymentMethodIsDisplayed() {
        val savedPaymentMethods = PaymentMethodFixtures.createCards(3)
        val displayedPaymentMethod = savedPaymentMethods[2]
        val paymentMethodTypes = listOf("card", "cashapp")
        var currentlySelectedPaymentMethodCode: PaymentMethodCode? = null
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes
                )
            ),
            initialPaymentMethods = savedPaymentMethods,
            initialMostRecentlySelectedSavedPaymentMethod = displayedPaymentMethod,
            onFormFieldValuesChanged = { _, selectedPaymentMethodCode ->
                currentlySelectedPaymentMethodCode = selectedPaymentMethodCode
            },
            formElementsForCode = { _ -> emptyList() }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
            interactor.state.test {
                awaitItem().run {
                    assertThat(displayedSavedPaymentMethod).isNotNull()
                    assertThat(displayedSavedPaymentMethod!!.paymentMethod).isEqualTo(displayedPaymentMethod)
                    assertThat(currentlySelectedPaymentMethodCode).isEqualTo("cashapp")
                }
            }
        }
    }

    @Test
    fun handleViewAction_TransitionToManageSavedPaymentMethods_transitionsToManageScreen() {
        var calledManageScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            manageScreenFactory = {
                calledManageScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.TransitionToManageSavedPaymentMethods)
            assertThat(calledManageScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_TransitionToManageOneSavedPaymentMethod_transitionsToManageOnSavedPMScreen() {
        var calledManageOneSavedPaymentMethodScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            manageOneSavedPaymentMethodFactory = {
                calledManageOneSavedPaymentMethodScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.TransitionToManageOneSavedPaymentMethod)
            assertThat(calledManageOneSavedPaymentMethodScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_SelectSavedPaymentMethod_selectsSavedPm() {
        val savedPaymentMethod = PaymentMethodFixtures.displayableCard()
        var selectedSavedPaymentMethod: PaymentMethod? = null
        runScenario(
            onSelectSavedPaymentMethod = { selectedSavedPaymentMethod = it }
        ) {
            interactor.handleViewAction(ViewAction.SavedPaymentMethodSelected(savedPaymentMethod.paymentMethod))
            assertThat(selectedSavedPaymentMethod).isEqualTo(savedPaymentMethod.paymentMethod)
        }
    }

    @Test
    fun verticalModeScreenSelection_isNeverUpdatedToNull() {
        val expectedPaymentSelection = PaymentSelection.Link
        runScenario(initialSelection = expectedPaymentSelection, updateSelection = {}) {
            selectionSource.value = null

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(expectedPaymentSelection)
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_isNeverUpdatedToNewPmWithFormFields() {
        val initialPaymentSelection = PaymentSelection.Link
        runScenario(
            initialSelection = initialPaymentSelection,
            updateSelection = {},
            formElementsForCode = { formFieldsWhichRequireUserInteraction }
        ) {
            selectionSource.value = PaymentMethodFixtures.CARD_PAYMENT_SELECTION

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(initialPaymentSelection)
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeANewPmWithoutFormFields() {
        val initialPaymentSelection = PaymentSelection.Link
        runScenario(
            initialSelection = initialPaymentSelection,
            updateSelection = {},
            formElementsForCode = { emptyList() }
        ) {
            val newSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            selectionSource.value = newSelection

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(newSelection)
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeAnEpm() {
        val initialPaymentSelection = PaymentSelection.Link
        runScenario(
            initialSelection = initialPaymentSelection,
            updateSelection = {},
            formElementsForCode = { emptyList() }
        ) {
            val newSelection = PaymentMethodFixtures.createExternalPaymentMethod(
                PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
            )
            selectionSource.value = newSelection

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(newSelection)
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeASavedPm() {
        val initialPaymentSelection = PaymentSelection.Link
        runScenario(
            initialSelection = initialPaymentSelection,
            updateSelection = {},
            formElementsForCode = { emptyList() }
        ) {
            val newSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            selectionSource.value = newSelection

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(newSelection)
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeLink() {
        val initialPaymentSelection = PaymentSelection.GooglePay
        runScenario(
            initialSelection = initialPaymentSelection,
            updateSelection = {},
            formElementsForCode = { emptyList() }
        ) {
            val newSelection = PaymentSelection.Link
            selectionSource.value = newSelection

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(newSelection)
                }
            }
        }
    }

    @Test
    fun verticalModeScreenSelection_canBeGooglePay() {
        val initialPaymentSelection = PaymentSelection.Link
        runScenario(
            initialSelection = initialPaymentSelection,
            updateSelection = {},
            formElementsForCode = { emptyList() }
        ) {
            val newSelection = PaymentSelection.GooglePay
            selectionSource.value = newSelection

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selection).isEqualTo(newSelection)
                }
            }
        }
    }

    @Test
    fun whenVerticalModeScreen_becomesCurrentScreen_updateSelectionCalled() {
        var updatedSelection: PaymentSelection? = null
        fun onUpdateSelection(paymentSelection: PaymentSelection?) {
            updatedSelection = paymentSelection
        }
        val verticalModeSelection = PaymentSelection.GooglePay

        runScenario(
            initialIsCurrentScreen = false,
            initialSelection = verticalModeSelection,
            updateSelection = ::onUpdateSelection,
            formElementsForCode = { emptyList() },
        ) {
            isCurrentScreenSource.value = true

            dispatcher.scheduler.advanceUntilIdle()

            assertThat(updatedSelection).isEqualTo(verticalModeSelection)
        }
    }

    private val formFieldsWhichRequireUserInteraction =
        listOf(SaveForFutureUseElement(true, "Jay's Ski Shop"))

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            cbcEligibility = CardBrandChoiceEligibility.create(
                isEligible = true,
                preferredNetworks = emptyList()
            )
        ),
        initialProcessing: Boolean = false,
        initialSelection: PaymentSelection? = null,
        initialIsCurrentScreen: Boolean = true,
        formElementsForCode: (code: String) -> List<FormElement> = { notImplemented() },
        transitionTo: (screen: PaymentSheetScreen) -> Unit = { notImplemented() },
        onFormFieldValuesChanged: (formValues: FormFieldValues, selectedPaymentMethodCode: String) -> Unit = { _, _ ->
            notImplemented()
        },
        manageScreenFactory: () -> PaymentSheetScreen = { notImplemented() },
        manageOneSavedPaymentMethodFactory: () -> PaymentSheetScreen = { notImplemented() },
        formScreenFactory: (selectedPaymentMethodCode: String) -> PaymentSheetScreen = { notImplemented() },
        initialPaymentMethods: List<PaymentMethod>? = null,
        initialMostRecentlySelectedSavedPaymentMethod: PaymentMethod? = null,
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit = { notImplemented() },
        onSelectSavedPaymentMethod: (PaymentMethod) -> Unit = { notImplemented() },
        isFlowController: Boolean = false,
        updateSelection: (PaymentSelection?) -> Unit = { notImplemented() },
        onMandateTextUpdated: (ResolvableString?) -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val processing: MutableStateFlow<Boolean> = MutableStateFlow(initialProcessing)
        val selection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(initialSelection)
        val paymentMethods: MutableStateFlow<List<PaymentMethod>?> = MutableStateFlow(initialPaymentMethods)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> =
            MutableStateFlow(initialMostRecentlySelectedSavedPaymentMethod)
        val walletsState = MutableStateFlow<WalletsState?>(null)
        val isCurrentScreen: MutableStateFlow<Boolean> = MutableStateFlow(initialIsCurrentScreen)
        val dispatcher = StandardTestDispatcher()

        val interactor = DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = processing,
            selection = selection,
            formElementsForCode = formElementsForCode,
            transitionTo = transitionTo,
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            manageScreenFactory = manageScreenFactory,
            manageOneSavedPaymentMethodFactory = manageOneSavedPaymentMethodFactory,
            formScreenFactory = formScreenFactory,
            paymentMethods = paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
            providePaymentMethodName = { it!! },
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
            onEditPaymentMethod = onEditPaymentMethod,
            onSelectSavedPaymentMethod = onSelectSavedPaymentMethod,
            walletsState = walletsState,
            isFlowController = isFlowController,
            onMandateTextUpdated = onMandateTextUpdated,
            updateSelection = updateSelection,
            isCurrentScreen = isCurrentScreen,
            dispatcher = dispatcher,
        )

        TestParams(
            processingSource = processing,
            selectionSource = selection,
            isCurrentScreenSource = isCurrentScreen,
            mostRecentlySelectedSavedPaymentMethodSource = mostRecentlySelectedSavedPaymentMethod,
            walletsState = walletsState,
            interactor = interactor,
            dispatcher = dispatcher,
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private class TestParams(
        val processingSource: MutableStateFlow<Boolean>,
        val selectionSource: MutableStateFlow<PaymentSelection?>,
        val isCurrentScreenSource: MutableStateFlow<Boolean>,
        val mostRecentlySelectedSavedPaymentMethodSource: MutableStateFlow<PaymentMethod?>,
        val walletsState: MutableStateFlow<WalletsState?>,
        val interactor: PaymentMethodVerticalLayoutInteractor,
        val dispatcher: TestDispatcher,
    )
}
