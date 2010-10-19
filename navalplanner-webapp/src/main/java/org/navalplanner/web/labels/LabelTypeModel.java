/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
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

package org.navalplanner.web.labels;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.common.IntegrationEntity;
import org.navalplanner.business.common.daos.IConfigurationDAO;
import org.navalplanner.business.common.entities.EntityNameEnum;
import org.navalplanner.business.common.exceptions.InstanceNotFoundException;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.labels.daos.ILabelTypeDAO;
import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.labels.entities.LabelType;
import org.navalplanner.web.common.IntegrationEntityModel;
import org.navalplanner.web.common.concurrentdetection.OnConcurrentModification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/labels/labelTypes.zul")
public class LabelTypeModel extends IntegrationEntityModel implements
        ILabelTypeModel {

    @Autowired
    private ILabelTypeDAO labelTypeDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    private LabelType labelType;

    public LabelTypeModel() {

    }

    @Override
    @Transactional(readOnly=true)
    public List<LabelType> getLabelTypes() {
        return labelTypeDAO.getAll();
    }

    @Override
    @Transactional
    public void confirmDelete(LabelType labelType) {
        try {
            labelTypeDAO.remove(labelType.getId());
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException();
        }
    }

    @Override
    @Transactional(readOnly=true)
    public void initCreate() {
        boolean codeGenerated = configurationDAO.getConfiguration()
                .getGenerateCodeForLabel();
        labelType = LabelType.create("", "");

        if (codeGenerated) {
            labelType.setCodeAutogenerated(codeGenerated);
            setDefaultCode();
        }
    }

    @Override
    public LabelType getLabelType() {
        return labelType;
    }

    @Override
    @Transactional
    public void confirmSave() throws ValidationException {
        checkLabelTypeUnique();
        checkLabelsUnique();

        if (labelType.isCodeAutogenerated()) {
            generateLabelCodes();
        }
        labelTypeDAO.save(labelType);
    }

    private void generateLabelCodes() {
        labelType.generateLabelCodes(getNumberOfDigitsCode());
    }

    private void checkLabelTypeUnique() {
        if (!labelTypeDAO.isUnique(labelType)) {
            throw new ValidationException(createInvalidValue(labelType));
        }
    }

    private InvalidValue createInvalidValue(LabelType labelType) {
        return new InvalidValue(_(
                "{0} already exists", labelType.getName()),
                LabelType.class, "name", labelType.getName(), labelType);
    }

    /**
     * Check {@link Label} name is unique
     *
     * @return
     * @throws ValidationException
     */
    private void checkLabelsUnique() throws ValidationException {
        List<InvalidValue> result = new ArrayList<InvalidValue>();
        List<Label> labels = new ArrayList<Label>(labelType.getLabels());
        for (int i = 0; i < labels.size(); i++) {
            for (int j = i + 1; j < labels.size(); j++) {
                if (labels.get(j).getName().equals(labels.get(i).getName())) {
                    result.add(createInvalidValue(labels.get(j)));
                }
            }
        }
        if (!result.isEmpty()) {
            throw new ValidationException(result.toArray(new InvalidValue[0]));
        }
    }

    private InvalidValue createInvalidValue(Label label) {
        return new InvalidValue(_(
                "{0} already exists", label.getName()),
                LabelType.class, "name", label.getName(), label);
    }

    @Override
    @Transactional(readOnly = true)
    public void initEdit(LabelType labelType) {
        Validate.notNull(labelType);
        this.labelType = getFromDB(labelType);
        initOldCodes();
    }

    private LabelType getFromDB(LabelType labelType) {
        return getFromDB(labelType.getId());
    }

    private LabelType getFromDB(Long id) {
        try {
            LabelType result = labelTypeDAO.find(id);
            reattachLabels(result);
            return result;
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void reattachLabels(LabelType labelType) {
        for (Label label : labelType.getLabels()) {
            label.getName();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Label> getLabels() {
        // Safe copy
        List<Label> labels = new ArrayList<Label>();
        if (labelType != null) {
            labels.addAll(labelType.getLabels());
        }
        return labels;
    }

    @Override
    public void addLabel(String value) {
        Label label = Label.create("", value);
        label.setType(labelType);
        labelType.addLabel(label);
    }

    @Override
    public void confirmDeleteLabel(Label label) {
        labelType.removeLabel(label);
    }

    @Override
    public boolean labelNameIsUnique(String name) {
        int count = 0;

        for (Label label : labelType.getLabels()) {
            if (name.equals(label.getName())) {
                count++;
            }
        }
        return (count == 1);
    }

    public EntityNameEnum getEntityName() {
        return EntityNameEnum.LABEL;
    }

    public Set<IntegrationEntity> getChildren() {
        return (Set<IntegrationEntity>) (labelType != null ? labelType
                .getLabels() : new ArrayList<IntegrationEntity>());
    }

    public IntegrationEntity getCurrentEntity() {
        return this.labelType;
    }
}
