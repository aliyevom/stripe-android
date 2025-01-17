package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class DefaultSelectSavedPaymentMethodsInteractorTest {

    @Test
    fun initialState_isCorrect() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val expectedPaymentOptionsItems = createPaymentOptionsItems(paymentMethods)
        val expectedSelectedPaymentMethod = paymentMethods[1]
        val expectedIsEditing = true
        val expectedIsProcessing = false

        runScenario(
            paymentOptionsItems = MutableStateFlow(expectedPaymentOptionsItems),
            currentSelection = MutableStateFlow(PaymentSelection.Saved(expectedSelectedPaymentMethod)),
            editing = MutableStateFlow(expectedIsEditing),
            isProcessing = MutableStateFlow(expectedIsProcessing),
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsItems).isEqualTo(expectedPaymentOptionsItems)
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(expectedSelectedPaymentMethod)
                    assertThat(isEditing).isEqualTo(expectedIsEditing)
                    assertThat(isProcessing).isEqualTo(expectedIsProcessing)
                }
            }
        }
    }

    @Test
    fun updatingIsEditing_updatesState() {
        val initialIsEditingValue = false
        val isEditingFlow = MutableStateFlow(initialIsEditingValue)

        runScenario(editing = isEditingFlow) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(isEditing).isEqualTo(initialIsEditingValue)
                }
            }

            isEditingFlow.value = !initialIsEditingValue

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(isEditing).isEqualTo(!initialIsEditingValue)
                }
            }
        }
    }

    @Test
    fun updatingIsProcessing_updatesState() {
        val initialIsProcessingValue = false
        val isProcessingFlow = MutableStateFlow(initialIsProcessingValue)

        runScenario(isProcessing = isProcessingFlow) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(isProcessing).isEqualTo(initialIsProcessingValue)
                }
            }

            isProcessingFlow.value = !initialIsProcessingValue

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(isProcessing).isEqualTo(!initialIsProcessingValue)
                }
            }
        }
    }

    @Test
    fun updatingPaymentOptionsState_updatesState() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val initialPaymentOptionsItems = createPaymentOptionsItems(paymentMethods)
        val paymentOptionsStateFlow = MutableStateFlow(initialPaymentOptionsItems)

        runScenario(paymentOptionsStateFlow) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsItems).isEqualTo(initialPaymentOptionsItems)
                }
            }

            val newPaymentMethods = PaymentMethodFixtures.createCards(2)
            val newPaymentOptionsState = createPaymentOptionsItems(newPaymentMethods)
            paymentOptionsStateFlow.value = newPaymentOptionsState

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsItems).isEqualTo(newPaymentOptionsState)
                }
            }
        }
    }

    @Test
    fun handleViewAction_DeletePaymentMethod_deletesPaymentMethod() {
        var deletedPaymentMethod: PaymentMethod? = null
        fun onDeletePaymentMethod(paymentMethod: PaymentMethod) {
            deletedPaymentMethod = paymentMethod
        }

        runScenario(onDeletePaymentMethod = ::onDeletePaymentMethod) {
            val paymentMethodToDelete = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.DeletePaymentMethod(
                    paymentMethodToDelete
                )
            )

            assertThat(deletedPaymentMethod).isEqualTo(paymentMethodToDelete)
        }
    }

    @Test
    fun handleViewAction_EditPaymentMethod_editsPaymentMethod() {
        var editedPaymentMethod: PaymentMethod? = null
        fun onEditPaymentMethod(paymentMethod: PaymentMethod) {
            editedPaymentMethod = paymentMethod
        }

        runScenario(onEditPaymentMethod = ::onEditPaymentMethod) {
            val paymentMethodToEdit = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.EditPaymentMethod(
                    paymentMethodToEdit
                )
            )

            assertThat(editedPaymentMethod).isEqualTo(paymentMethodToEdit)
        }
    }

    @Test
    fun handleViewAction_SelectPaymentMethod_selectsPaymentMethod() {
        var paymentSelection: PaymentSelection? = null
        fun onSelectPaymentMethod(selection: PaymentSelection?) {
            paymentSelection = selection
        }

        runScenario(onPaymentMethodSelected = ::onSelectPaymentMethod) {
            val newPaymentSelection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.SelectPaymentMethod(
                    newPaymentSelection
                )
            )

            assertThat(paymentSelection).isEqualTo(newPaymentSelection)
        }
    }

    @Test
    fun handleViewAction_AddCardPressed_callsOnAddCardPressed() {
        var addCardPressed = false
        fun onAddCardPressed() {
            addCardPressed = true
        }

        runScenario(
            onAddCardPressed = ::onAddCardPressed
        ) {
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.AddCardPressed
            )

            assertThat(addCardPressed).isTrue()
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsLink() {
        val currentSelectionFlow = MutableStateFlow(PaymentSelection.Link)

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(
                    paymentMethods = PaymentMethodFixtures.createCards(2),
                ).plus(PaymentOptionsItem.Link)
            ),
            currentSelection = currentSelectionFlow,
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsLink_canBeChangedToGooglePay() {
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> =
            MutableStateFlow(PaymentSelection.Link)

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(
                    paymentMethods = PaymentMethodFixtures.createCards(2),
                ).plus(PaymentOptionsItem.Link).plus(PaymentOptionsItem.GooglePay)
            ),
            currentSelection = currentSelectionFlow,
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }

            currentSelectionFlow.value = PaymentSelection.GooglePay
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.GooglePay)
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsLink_doesNotChangeWhenSelectionBecomesNew() {
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> =
            MutableStateFlow(PaymentSelection.Link)

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(
                    paymentMethods = PaymentMethodFixtures.createCards(2),
                ).plus(PaymentOptionsItem.Link)
            ),
            currentSelection = currentSelectionFlow,
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }

            currentSelectionFlow.value = newPaymentSelection()
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsNull_usesMostRecentlySavedSelection() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(createPaymentOptionsItems(paymentMethods = paymentMethods)),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        selectedSavedPaymentMethod
                    )
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsNull_respondsToChangesToMostRecentlySavedSelection() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(createPaymentOptionsItems(paymentMethods = paymentMethods)),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        selectedSavedPaymentMethod
                    )
                }
            }

            mostRecentlySelectedSavedPaymentMethod.value = paymentMethods[0]
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        paymentMethods[0]
                    )
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_canChangeFromSaved_toLink() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(paymentMethods = paymentMethods).plus(
                    PaymentOptionsItem.Link
                )
            ),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        selectedSavedPaymentMethod
                    )
                }
            }

            currentSelectionFlow.value = PaymentSelection.Link
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(
                        PaymentOptionsItem.Link
                    )
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_savedPaymentSelectionRemoved_newSelectionIsNull() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod: PaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(createPaymentOptionsItems(paymentMethods = paymentMethods)),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(selectedSavedPaymentMethod)
                }
            }

            currentSelectionFlow.value = null
            mostRecentlySelectedSavedPaymentMethod.value = null
            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isNull()
                }
            }
        }
    }

    private fun createPaymentOptionsItems(
        paymentMethods: List<PaymentMethod>,
    ): List<PaymentOptionsItem> {
        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            nameProvider = { it!! },
            canRemovePaymentMethods = true,
            isCbcEligible = true,
        ).items
    }

    private fun newPaymentSelection(): PaymentSelection.New {
        return PaymentSelection.New.USBankAccount(
            labelResource = "Test",
            iconResource = 0,
            paymentMethodCreateParams = mock(),
            customerRequestedSave = mock(),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = null,
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
            instantDebits = null,
            screenState = USBankAccountFormScreenState.SavedAccount(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = resolvableString("Continue"),
                mandateText = null,
            ),
        )
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        paymentOptionsItems: StateFlow<List<PaymentOptionsItem>> = MutableStateFlow(emptyList()),
        editing: StateFlow<Boolean> = MutableStateFlow(false),
        isProcessing: StateFlow<Boolean> = MutableStateFlow(false),
        currentSelection: StateFlow<PaymentSelection?> = MutableStateFlow(null),
        mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(null),
        onAddCardPressed: () -> Unit = { notImplemented() },
        onEditPaymentMethod: (PaymentMethod) -> Unit = { notImplemented() },
        onDeletePaymentMethod: (PaymentMethod) -> Unit = { notImplemented() },
        onPaymentMethodSelected: (PaymentSelection?) -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

        val interactor = DefaultSelectSavedPaymentMethodsInteractor(
            paymentOptionsItems,
            editing,
            isProcessing = isProcessing,
            currentSelection = currentSelection,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
            onAddCardPressed,
            onEditPaymentMethod,
            onDeletePaymentMethod,
            onPaymentMethodSelected,
            dispatcher = dispatcher,
        )

        TestParams(
            interactor = interactor,
            dispatcher = dispatcher,
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private class TestParams(
        val interactor: SelectSavedPaymentMethodsInteractor,
        val dispatcher: TestDispatcher,
    )
}
