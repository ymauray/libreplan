/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.orders;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.orders.entities.OrderLine;
import org.navalplanner.business.orders.entities.OrderLineGroup;
import org.navalplanner.business.orders.entities.SchedulingState;
import org.navalplanner.business.requirements.entities.CriterionRequirement;
import org.navalplanner.business.templates.entities.OrderElementTemplate;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.Util.Getter;
import org.navalplanner.web.common.Util.Setter;
import org.navalplanner.web.common.components.bandboxsearch.BandboxMultipleSearch;
import org.navalplanner.web.common.components.bandboxsearch.BandboxSearch;
import org.navalplanner.web.common.components.finders.FilterPair;
import org.navalplanner.web.orders.assigntemplates.TemplateFinderPopup;
import org.navalplanner.web.orders.assigntemplates.TemplateFinderPopup.IOnResult;
import org.navalplanner.web.templates.IOrderTemplatesControllerEntryPoints;
import org.navalplanner.web.tree.TreeController;
import org.zkoss.ganttz.IPredicate;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.KeyEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.api.Treecell;
import org.zkoss.zul.api.Treerow;
import org.zkoss.zul.impl.api.InputElement;

/**
 * Controller for {@link OrderElement} tree view of {@link Order} entities <br />
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class OrderElementTreeController extends TreeController<OrderElement> {

    private Vbox orderElementFilter;

    private Hbox orderFilter;

    private BandboxMultipleSearch bdFiltersOrderElement;

    private Datebox filterStartDateOrderElement;

    private Datebox filterFinishDateOrderElement;

    private Textbox filterNameOrderElement;

    private OrderElementTreeitemRenderer renderer = new OrderElementTreeitemRenderer();

    private final IOrderModel orderModel;

    private final OrderElementController orderElementController;

    private transient IPredicate predicate;

    @Resource
    private IOrderTemplatesControllerEntryPoints orderTemplates;

    private final IMessagesForUser messagesForUser;

    public List<org.navalplanner.business.labels.entities.Label> getLabels() {
        return orderModel.getLabels();
    }

    @Override
    public OrderElementTreeitemRenderer getRenderer() {
        return renderer;
    }

    public OrderElementTreeController(IOrderModel orderModel,
            OrderElementController orderElementController,
            IMessagesForUser messagesForUser) {
        super(OrderElement.class);
        this.orderModel = orderModel;
        this.orderElementController = orderElementController;
        this.messagesForUser = messagesForUser;
    }

    public OrderElementController getOrderElementController() {
        return orderElementController;
    }

    @Override
    protected OrderElementTreeModel getModel() {
        return orderModel.getOrderElementTreeModel();
    }

    public void createTemplate() {
        if (tree.getSelectedCount() == 1) {
            createTemplate(getSelectedNode());
        } else {
            try {
                Messagebox.show(_("Choose a order element "
                        + "from which create a template from"));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isTemplateCreationConfirmed() {
        try {
            int status = Messagebox
                    .show(
                            _("Still not saved changes would be lost."
                                    + " Are you sure you want to go to create a template?"),
                            "Confirm", Messagebox.YES | Messagebox.NO,
                            Messagebox.QUESTION);
            return Messagebox.YES == status;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void createFromTemplate() {
        templateFinderPopup.openForSubElemenetCreation(tree, "after_pointer",
                new IOnResult<OrderElementTemplate>() {
                    @Override
                    public void found(OrderElementTemplate template) {
                        OrderLineGroup parent = (OrderLineGroup) getModel()
                                .getRoot();
                        OrderElement created = orderModel.createFrom(parent,
                                template);
                        getModel().addNewlyAddedChildrenOf(parent);
                    }
                });
    }

    private void createTemplate(OrderElement selectedNode) {
        if (!isTemplateCreationConfirmed()) {
            return;
        }
        if (!selectedNode.isNewObject()) {
            orderTemplates.goToCreateTemplateFrom(selectedNode);
        } else {
            notifyTemplateCantBeCreated();
        }
    }

    private void notifyTemplateCantBeCreated() {
        try {
            Messagebox
                    .show(
                            _("Templates can only be created from already existent order elements.\n"
                                    + "Newly order elements cannot be used."),
                            _("Operation cannot be done"), Messagebox.OK,
                            Messagebox.INFORMATION);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void notifyDateboxCantBeCreated(final String dateboxName,
            final String codeOrderElement) {
        try {
            Messagebox.show(_("the " + dateboxName
                    + "datebox of the order element " + codeOrderElement
                    + " could not be created.\n"),
                    _("Operation cannot be done"), Messagebox.OK,
                    Messagebox.INFORMATION);
        } catch (InterruptedException e) {
        }
    }

    protected void filterByPredicateIfAny() {
        if (predicate != null) {
            filterByPredicate();
        }
    }

    private void filterByPredicate() {
        OrderElementTreeModel orderElementTreeModel = orderModel
                .getOrderElementsFilteredByPredicate(predicate);
        tree.setModel(orderElementTreeModel.asTree());
        tree.invalidate();
    }

    void doEditFor(Order order) {
        Util.reloadBindings(tree);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // Configuration of the order elements filter
        Component filterComponent = Executions.createComponents(
                "/orders/_orderElementTreeFilter.zul", orderElementFilter,
                new HashMap<String, String>());
        filterComponent.setVariable("treeController", this, true);
        bdFiltersOrderElement = (BandboxMultipleSearch) filterComponent
                .getFellow("bdFiltersOrderElement");
        filterStartDateOrderElement = (Datebox) filterComponent
                .getFellow("filterStartDateOrderElement");
        filterFinishDateOrderElement = (Datebox) filterComponent
                .getFellow("filterFinishDateOrderElement");
        filterNameOrderElement = (Textbox) filterComponent
                .getFellow("filterNameOrderElement");

        templateFinderPopup = (TemplateFinderPopup) comp
                .getFellow("templateFinderPopupAtTree");
    }

    private enum Navigation {
        LEFT, UP, RIGHT, DOWN;
        public static Navigation getIntentFrom(KeyEvent keyEvent) {
            return values()[keyEvent.getKeyCode() - 37];
        }
    }

    public class OrderElementTreeitemRenderer extends Renderer {

        private Map<OrderElement, Intbox> hoursIntBoxByOrderElement = new HashMap<OrderElement, Intbox>();

        public OrderElementTreeitemRenderer() {
        }

        private void registerKeyboardListener(final InputElement inputElement) {
            inputElement.setCtrlKeys("#up#down#left#right");
            inputElement.addEventListener("onCtrlKey", new EventListener() {
                private Treerow treerow = getCurrentTreeRow();

                @Override
                public void onEvent(Event event) throws Exception {
                    Navigation navigation = Navigation.getIntentFrom((KeyEvent)event);
                    moveFocusTo(inputElement, navigation, treerow);
                }
            });
        }

        private void moveFocusTo(InputElement inputElement, Navigation navigation, Treerow treerow) {
            List<InputElement> boxes = getBoxes(treerow);
            int position = boxes.indexOf(inputElement);

            switch (navigation) {
            case UP:
                focusGoUp(treerow, position);
                break;
            case DOWN:
                focusGoDown(treerow, position);
                break;
            case LEFT:
                if (position == 0) {
                    focusGoUp(treerow, boxes.size() - 1);
                } else {
                    if(boxes.get(position - 1).isDisabled()) {
                        moveFocusTo(boxes.get(position - 1), Navigation.LEFT, treerow);
                    }
                    else {
                        boxes.get(position - 1).focus();
                    }
                }
                break;
            case RIGHT:
                if (position == boxes.size() - 1) {
                    focusGoDown(treerow, 0);
                } else {
                    if(boxes.get(position + 1).isDisabled()) {
                        moveFocusTo(boxes.get(position + 1), Navigation.RIGHT, treerow);
                    }
                    else {
                        boxes.get(position + 1).focus();
                    }
                }
                break;
            }
        }

        private void focusGoUp(Treerow treerow, int position) {
            List treeItems = treerow.getParent().getParent().getChildren();
            int myPosition = treeItems.indexOf(treerow.getParent());

            if(myPosition > 0) {
                Treerow upTreerow = (Treerow)
                    ((Component)treeItems.get(myPosition - 1)).getChildren().get(0);
                List<InputElement> boxes = getBoxes(upTreerow);

                if(boxes.get(position).isDisabled()) {
                    moveFocusTo(boxes.get(position), Navigation.LEFT, upTreerow);
                }
                else {
                    boxes.get(position).focus();
                }
            }
        }

        private void focusGoDown(Treerow treerow, int position) {
            List treeItems = treerow.getParent().getParent().getChildren();
            int myPosition = treeItems.indexOf(treerow.getParent());

            if(myPosition < treeItems.size() - 1) {
                Treerow downTreerow = (Treerow)
                    ((Component)treeItems.get(myPosition + 1)).getChildren().get(0);
                List<InputElement> boxes = getBoxes(downTreerow);

                if(boxes.get(position).isDisabled()) {
                    moveFocusTo(boxes.get(position), Navigation.RIGHT, downTreerow);
                }
                else {
                    boxes.get(position).focus();
                }
            }
        }

        private List<InputElement> getBoxes(Treerow row) {
            InputElement codeBox = (InputElement)
                ((Treecell)row.getChildren().get(1)).getChildren().get(0);
            InputElement nameBox = (InputElement)
                ((Treecell)row.getChildren().get(2)).getChildren().get(1);
            InputElement hoursBox = (InputElement)
                ((Treecell)row.getChildren().get(3)).getChildren().get(0);
            InputElement initDateBox = (InputElement)
                ((Hbox)((Treecell)row.getChildren().get(4)).getChildren().get(0)).getChildren().get(0);
            InputElement endDateBox = (InputElement)
                ((Hbox)((Treecell)row.getChildren().get(5)).getChildren().get(0)).getChildren().get(0);

            return Arrays.asList(codeBox, nameBox, hoursBox, initDateBox, endDateBox);
        }

        @Override
        protected void addDescriptionCell(OrderElement element) {
            addTaskNumberCell(element);
        }

        private void addTaskNumberCell(final OrderElement orderElementForThisRow) {
            int[] path = getModel().getPath(orderElementForThisRow);
            String cssClass = "depth_" + path.length;

            Label taskNumber = new Label(pathAsString(path));
            taskNumber.setSclass("tasknumber");
            taskNumber.addEventListener(Events.ON_DOUBLE_CLICK,
                    new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            IOrderElementModel model = orderModel
                                    .getOrderElementModel(orderElementForThisRow);
                            orderElementController.openWindow(model);
                            // Util.reloadBindings(tree);
                        }

                    });

            // TODO It would be needed to expand the width for the numbers
            // to make it ready for 2 and 3 digit numbers
            Textbox textBox = Util.bind(new Textbox(),
                    new Util.Getter<String>() {

                        @Override
                        public String get() {
                            return orderElementForThisRow.getName();
                        }
                    }, new Util.Setter<String>() {

                        @Override
                        public void set(String value) {
                            orderElementForThisRow.setName(value);
                        }
                    });
            if (readOnly) {
                textBox.setDisabled(true);
            }
            addCell(cssClass, taskNumber, textBox);
            registerKeyboardListener(textBox);
        }

        @Override
        protected SchedulingState getSchedulingStateFrom(
                OrderElement currentElement) {
            return currentElement.getSchedulingState();
        }


        @Override
        protected void onDoubleClickForSchedulingStateCell(
                final OrderElement currentOrderElement) {
            IOrderElementModel model = orderModel
                    .getOrderElementModel(currentOrderElement);
            orderElementController.openWindow(model);
        }

        protected void addCodeCell(final OrderElement orderElement) {
            Textbox textBoxCode = new Textbox();
            Util.bind(textBoxCode, new Util.Getter<String>() {
                @Override
                public String get() {
                    return orderElement.getCode();
                }
            }, new Util.Setter<String>() {

                @Override
                public void set(String value) {
                    orderElement.setCode(value);
                }
            });
            textBoxCode.setConstraint(new Constraint() {
                @Override
                public void validate(Component comp, Object value)
                        throws WrongValueException {
                    if (!orderElement.isFormatCodeValid((String) value)) {
                        throw new WrongValueException(
                                comp,
                                _("Value is not valid.\n Code cannot contain chars like '_' \n and should not be empty"));
                    }
                }
            });

            if (orderModel.isCodeAutogenerated() || readOnly) {
                textBoxCode.setDisabled(true);
            }

            addCell(textBoxCode);
            registerKeyboardListener(textBoxCode);
        }

        void addInitDateCell(final OrderElement currentOrderElement) {
            DynamicDatebox dinamicDatebox = new DynamicDatebox(
                    currentOrderElement, new DynamicDatebox.Getter<Date>() {

                        @Override
                        public Date get() {
                            return currentOrderElement.getInitDate();
                        }
                    }, new DynamicDatebox.Setter<Date>() {

                        @Override
                        public void set(Date value) {
                            currentOrderElement.setInitDate(value);

                        }
                    });
            if (readOnly) {
                dinamicDatebox.setDisabled(true);
            }
            addDateCell(dinamicDatebox, _("init"), currentOrderElement);
            registerKeyboardListener(dinamicDatebox.getDateTextBox());
        }

        void addEndDateCell(final OrderElement currentOrderElement) {
            DynamicDatebox dinamicDatebox = new DynamicDatebox(
                    currentOrderElement, new DynamicDatebox.Getter<Date>() {

                        @Override
                        public Date get() {
                            return currentOrderElement.getDeadline();
                        }
                    }, new DynamicDatebox.Setter<Date>() {

                        @Override
                        public void set(Date value) {
                            currentOrderElement.setDeadline(value);
                        }
                    });
            if (readOnly) {
                dinamicDatebox.setDisabled(true);
            }
            addDateCell(dinamicDatebox, _("end"), currentOrderElement);
            registerKeyboardListener(dinamicDatebox.getDateTextBox());
        }

        void addHoursCell(final OrderElement currentOrderElement) {
            Intbox intboxHours = buildHoursIntboxFor(currentOrderElement);
            hoursIntBoxByOrderElement.put(currentOrderElement, intboxHours);
            if (readOnly) {
                intboxHours.setDisabled(true);
            }
            addCell(intboxHours);
            registerKeyboardListener(intboxHours);
        }

        private void addDateCell(final DynamicDatebox dinamicDatebox,
                final String dateboxName,
                final OrderElement currentOrderElement) {

            Component cell = Executions.getCurrent().createComponents(
                    "/common/components/dynamicDatebox.zul", null, null);
            try {
                dinamicDatebox.doAfterCompose(cell);
            } catch (Exception e) {
                notifyDateboxCantBeCreated(dateboxName, currentOrderElement
                        .getCode());
            }
            addCell(cell);
        }

        private Intbox buildHoursIntboxFor(
                final OrderElement currentOrderElement) {
            Intbox result = new Intbox();
            if (currentOrderElement instanceof OrderLine) {
                OrderLine orderLine = (OrderLine) currentOrderElement;
                Util.bind(result, getHoursGetterFor(currentOrderElement),
                        getHoursSetterFor(orderLine));
                result.setConstraint(getHoursConstraintFor(orderLine));
            } else {
                // If it's a container hours cell is not editable
                Util.bind(result, getHoursGetterFor(currentOrderElement));
            }
            return result;
        }

        private Getter<Integer> getHoursGetterFor(
                final OrderElement currentOrderElement) {
            return new Util.Getter<Integer>() {
                @Override
                public Integer get() {
                    return currentOrderElement.getWorkHours();
                }
            };
        }

        private Constraint getHoursConstraintFor(final OrderLine orderLine) {
            return new Constraint() {
                @Override
                public void validate(Component comp, Object value)
                        throws WrongValueException {
                    if (!orderLine.isTotalHoursValid((Integer) value)) {
                        throw new WrongValueException(
                                comp,
                                _("Value is not valid, taking into account the current list of HoursGroup"));
                    }
                }
            };
        }

        private Setter<Integer> getHoursSetterFor(final OrderLine orderLine) {
            return new Util.Setter<Integer>() {
                @Override
                public void set(Integer value) {
                    orderLine.setWorkHours(value);
                    List<OrderElement> parentNodes = getModel().getParents(
                            orderLine);
                    // Remove the last element because it's an
                    // Order node, not an OrderElement
                    parentNodes.remove(parentNodes.size() - 1);
                    for (OrderElement node : parentNodes) {
                        Intbox intbox = hoursIntBoxByOrderElement.get(node);
                        intbox.setValue(node.getWorkHours());
                    }
                }
            };
        }

        @Override
        protected void addOperationsCell(final Treeitem item,
                final OrderElement currentOrderElement) {
            addCell(createEditButton(currentOrderElement, item),
                    createTemplateButton(currentOrderElement),
                    createDownButton(item,currentOrderElement),
                    createUpButton(item,currentOrderElement),
                    createUnindentButton(item, currentOrderElement),
                    createIndentButton(item, currentOrderElement),
                    createRemoveButton(currentOrderElement));
        }

        private Button createEditButton(final OrderElement currentOrderElement,
                final Treeitem item) {
            Button editbutton = createButton("/common/img/ico_editar1.png",
                    _("Edit"), "/common/img/ico_editar.png", "icono",
                    new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            showEditionOrderElement(item);
                        }
                    });
            return editbutton;
        }

        private Component createTemplateButton(
                final OrderElement currentOrderElement) {
            Button templateButton = createButton(
                    "/common/img/ico_derived1.png", _("Create Template"),
                    "/common/img/ico_derived.png",
                    "icono",
                    new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            createTemplate(currentOrderElement);
                        }
                    });
            return templateButton;
        }

    }

    @Override
    protected boolean isPredicateApplied() {
        return predicate != null;
    }

    /**
     * Apply filter to order elements in current order
     */
    public void onApplyFilter() {
        OrderElementPredicate predicate = createPredicate();
        this.predicate = predicate;

        if (predicate != null) {
            filterByPredicate(predicate);
        } else {
            showAllOrderElements();
        }
    }

    private OrderElementPredicate createPredicate() {
        List<FilterPair> listFilters = (List<FilterPair>) bdFiltersOrderElement
                .getSelectedElements();
        Date startDate = filterStartDateOrderElement.getValue();
        Date finishDate = filterFinishDateOrderElement.getValue();
        String name = filterNameOrderElement.getValue();

        if (listFilters.isEmpty() && startDate == null && finishDate == null
                && name == null) {
            return null;
        }
        return new OrderElementPredicate(listFilters, startDate, finishDate,
                name);
    }

    private void filterByPredicate(OrderElementPredicate predicate) {
        OrderElementTreeModel orderElementTreeModel = orderModel
                .getOrderElementsFilteredByPredicate(predicate);
        tree.setModel(orderElementTreeModel.asTree());
        tree.invalidate();
    }

    public void showAllOrderElements() {
        this.predicate = null;
        tree.setModel(orderModel.getOrderElementTreeModel().asTree());
        tree.invalidate();
    }

    @Override
    protected boolean isNewButtonDisabled() {
        if(readOnly) {
            return true;
        }
        return isPredicateApplied();
    }

    /**
     * Clear {@link BandboxSearch} for Labels, and initializes
     * {@link IPredicate}
     */
    public void clear() {
        selectDefaultTab();
        bdFiltersOrderElement.clear();
        predicate = null;
    }

    Tab tabGeneralData;

    private TemplateFinderPopup templateFinderPopup;

    private void selectDefaultTab() {
        tabGeneralData.setSelected(true);
    }

    @Override
    protected String createTooltipText(OrderElement elem) {
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append(elem.getName() + ". ");
        if ((elem.getDescription() != null)
                && (!elem.getDescription().equals(""))) {
            tooltipText.append(elem.getDescription());
            tooltipText.append(". ");
        }
        if ((elem.getLabels() != null) && (!elem.getLabels().isEmpty())) {
            tooltipText.append(" " + _("Labels") + ":");
            tooltipText.append(StringUtils.join(getLabels(), ","));
            tooltipText.append(".");
        }
        if ((elem.getCriterionRequirements() != null)
                && (!elem.getCriterionRequirements().isEmpty())) {
            ArrayList<String> criterionNames = new ArrayList<String>();
            for(CriterionRequirement each:elem.getCriterionRequirements()) {
                if (each.isValid()) {
                    criterionNames.add(each.getCriterion().getName());
                }
            }
            if (!criterionNames.isEmpty()) {
                tooltipText.append(" " + _("Criteria") + ":");
                tooltipText.append(StringUtils.join(criterionNames, ","));
                tooltipText.append(".");
            }
        }
        // To calculate other unit advances implement
        // getOtherAdvancesPercentage()
        tooltipText.append(" " + _("Advance") + ":" + elem.getAdvancePercentage());
        tooltipText.append(".");

        // tooltipText.append(elem.getAdvancePercentage());
        return tooltipText.toString();
    }

    public void showEditionOrderElement(final Treeitem item) {
        OrderElement currentOrderElement = (OrderElement) item.getValue();
        markModifiedTreeitem(item.getTreerow());
        IOrderElementModel model = orderModel
                .getOrderElementModel(currentOrderElement);
        orderElementController.openWindow(model);
    }

    public Treeitem getTreeitemByOrderElement(OrderElement element) {
        List<Treeitem> listItems = new ArrayList<Treeitem>(this.tree.getItems());
        for (Treeitem item : listItems) {
            OrderElement orderElement = (OrderElement) item.getValue();
            if (orderElement.getId().equals(element.getId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Operations to filter the orders by multiple filters
     */
    public Constraint checkConstraintFinishDate() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value)
                    throws WrongValueException {
                Date finishDate = (Date) value;
                if ((finishDate != null)
                        && (filterStartDateOrderElement.getValue() != null)
                        && (finishDate.compareTo(filterStartDateOrderElement
                                .getValue()) < 0)) {
                    filterFinishDateOrderElement.setValue(null);
                    throw new WrongValueException(comp,
                            _("must be greater than start date"));
                }
            }
        };
    }

    public Constraint checkConstraintStartDate() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value)
                    throws WrongValueException {
                Date startDate = (Date) value;
                if ((startDate != null)
                        && (filterFinishDateOrderElement.getValue() != null)
                        && (startDate.compareTo(filterFinishDateOrderElement
                                .getValue()) > 0)) {
                    filterStartDateOrderElement.setValue(null);
                    throw new WrongValueException(comp,
                            _("must be lower than finish date"));
                }
            }
        };
    }

    @Override
    protected void remove(OrderElement element) {
        boolean alreadyInUse = orderModel.isAlreadyInUse(element);
        if (alreadyInUse) {
            messagesForUser
                    .showMessage(
                            Level.ERROR,
                            _(
                                    "You can not remove the order element \"{0}\" because of this or any of its children are already in use in some work reports",
                                    element.getName()));
        } else {
            super.remove(element);
        }
    }

}
