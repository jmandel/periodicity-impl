import React from 'react'
import PropTypes from 'prop-types'
import { Alert } from 'react-native'
import { useTranslation } from 'react-i18next'

import { createCycleIgSampleCycleDays } from '../../../lib/cycle-ig/sample-data'
import {
  tryToImportWithoutDelete,
  updateCycleStartsForAllCycleDays,
} from '../../../db'
import alertError from '../common/alert-error'
import AppText from '../../common/app-text'
import Button from '../../common/button'
import Segment from '../../common/segment'

export default function LoadCycleIgSampleData({
  closePasswordConfirmation,
  setIsLoading,
}) {
  const { t } = useTranslation(null, {
    keyPrefix: 'sideMenu.settings.data.cycleIgSample',
  })

  function loadSampleData() {
    closePasswordConfirmation()
    setIsLoading(true)

    try {
      const cycleDays = createCycleIgSampleCycleDays()
      tryToImportWithoutDelete(cycleDays)
      updateCycleStartsForAllCycleDays()
      Alert.alert(
        t('successTitle'),
        t('successMessage', { count: cycleDays.length })
      )
    } catch (error) {
      alertError(t('error', { message: error.message }))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Segment title={t('title')}>
      <AppText>{t('text')}</AppText>
      <Button onPress={loadSampleData}>{t('button')}</Button>
    </Segment>
  )
}

LoadCycleIgSampleData.propTypes = {
  closePasswordConfirmation: PropTypes.func.isRequired,
  setIsLoading: PropTypes.func.isRequired,
}
