import React, { useState } from 'react'

import AppLoadingView from '../../common/app-loading'
import AppPage from '../../common/app-page'
import DeleteData from './DeleteData'
import ImportData from './ImportData'
import ExportData from './ExportData'
import ExportCycleFhirData from './ExportCycleFhirData'
import LoadCycleIgSampleData from './LoadCycleIgSampleData'

const DataManagement = () => {
  const [isLoading, setIsLoading] = useState(false)

  const [isPasswordConfirmationOpen, setIsPasswordConfirmationOpen] =
    useState(false)

  const openPasswordConfirmation = () => {
    setIsPasswordConfirmationOpen(true)
  }

  const closePasswordConfirmation = () => {
    setIsPasswordConfirmationOpen(false)
  }

  if (isLoading) return <AppLoadingView />

  return (
    <AppPage>
      <ExportData
        closePasswordConfirmation={closePasswordConfirmation}
        setIsLoading={setIsLoading}
      />
      <LoadCycleIgSampleData
        closePasswordConfirmation={closePasswordConfirmation}
        setIsLoading={setIsLoading}
      />
      <ExportCycleFhirData
        closePasswordConfirmation={closePasswordConfirmation}
      />
      <ImportData
        closePasswordConfirmation={closePasswordConfirmation}
        setIsLoading={setIsLoading}
      />
      <DeleteData
        openPasswordConfirmation={openPasswordConfirmation}
        closePasswordConfirmation={closePasswordConfirmation}
        isPasswordConfirmationOpen={isPasswordConfirmationOpen}
        setIsLoading={setIsLoading}
      />
    </AppPage>
  )
}

export default DataManagement
