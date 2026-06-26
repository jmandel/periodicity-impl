import React from 'react'
import PropTypes from 'prop-types'
import { useState } from 'react'
import { Linking, StyleSheet, View } from 'react-native'
import Clipboard from '@react-native-clipboard/clipboard'
import moment from 'moment'
import Svg, { Path } from 'react-native-svg'
import { getCycleDaysSortedByDate, mapRealmObjToJsObj } from '../../../db'
import {
  createCycleIgShare,
  revokeCycleIgShare,
} from '../../../lib/cycle-ig/share'
import {
  DEFAULT_CYCLE_IG_SCOPE,
  createCycleIgSnapshot,
} from '../../../lib/cycle-ig/snapshot'
import '../../../lib/cycle-ig/text-encoding'
import alertError from '../common/alert-error'
import { useTranslation } from 'react-i18next'
import QRCode from 'react-native-qrcode-svg'

import AppText from '../../common/app-text'
import AppTextInput from '../../common/app-text-input'
import Button from '../../common/button'
import AppSwitch from '../../common/app-switch'
import Segment from '../../common/segment'
import Share from 'react-native-share'

export default function ExportCycleFhirData({ closePasswordConfirmation }) {
  const [share, setShare] = useState(null)
  const [snapshot, setSnapshot] = useState(null)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [scope, setScope] = useState(DEFAULT_CYCLE_IG_SCOPE)
  const [hasCopied, setHasCopied] = useState(false)
  const [isBusy, setIsBusy] = useState(false)
  const { t } = useTranslation(null, {
    keyPrefix: 'sideMenu.settings.data.cycleFhirExport',
  })

  async function previewData() {
    if (isBusy) return

    try {
      const nextSnapshot = createSnapshot()
      setStartDate(nextSnapshot.startDate)
      setEndDate(nextSnapshot.endDate)
      setSnapshot(nextSnapshot)
      setHasCopied(false)
    } catch (error) {
      alertError(error.message)
    }
  }

  async function createShare() {
    if (isBusy) return

    try {
      closePasswordConfirmation()
      setIsBusy(true)
      await exportData(snapshot || createSnapshot())
    } catch (error) {
      alertError(error.message)
    } finally {
      setIsBusy(false)
    }
  }

  async function exportData(sourceSnapshot) {
    try {
      setSnapshot(sourceSnapshot)
      const createdShare = await createCycleIgShare(sourceSnapshot.cycleDays)
      setShare(createdShare)
    } catch (error) {
      alertError(error.message)
    }
  }

  function createSnapshot() {
    const data = loadDataFromDb()
    return createCycleIgSnapshot(data, {
      startDate: startDate.trim(),
      endDate: endDate.trim(),
      scope,
    })
  }

  function loadDataFromDb() {
    const cycleDaysByDate = mapRealmObjToJsObj(getCycleDaysSortedByDate())
    const hasNoData = cycleDaysByDate.length === 0
    if (hasNoData) {
      throw new Error(t('error.noData'))
    }
    return cycleDaysByDate
  }

  function updateScope(key, value) {
    if (share || isBusy) return

    setScope({
      ...scope,
      [key]: value,
    })
    setSnapshot(null)
    setHasCopied(false)
  }

  function copyLink() {
    Clipboard.setString(share.viewerLink)
    setHasCopied(true)
  }

  async function shareLink() {
    try {
      await Share.open({
        title: t('title'),
        message: share.viewerLink,
        subject: t('title'),
        type: 'text/plain',
        failOnCancel: false,
      })
    } catch (err) {
      alertError(t('error.sharingFailed'))
    }
  }

  async function openViewer() {
    try {
      await Linking.openURL(share.viewerLink)
    } catch (err) {
      alertError(t('error.openFailed'))
    }
  }

  async function revokeShare() {
    if (isBusy) return

    try {
      setIsBusy(true)
      await revokeCycleIgShare(share)
      setShare(null)
    } catch (err) {
      alertError(t('error.revokeFailed'))
    } finally {
      setIsBusy(false)
    }
  }

  return (
    <Segment title={t('button')}>
      <SmartLinkMark />
      <AppText>{t('text')}</AppText>
      <AppText style={styles.label}>{t('scope.user')}</AppText>
      <View style={styles.range}>
        <AppTextInput
          autoCapitalize="none"
          editable={!share && !isBusy}
          keyboardType="numbers-and-punctuation"
          onChangeText={(value) => {
            setStartDate(value)
            setSnapshot(null)
          }}
          placeholder="YYYY-MM-DD"
          style={styles.dateInput}
          value={startDate}
        />
        <AppTextInput
          autoCapitalize="none"
          editable={!share && !isBusy}
          keyboardType="numbers-and-punctuation"
          onChangeText={(value) => {
            setEndDate(value)
            setSnapshot(null)
          }}
          placeholder="YYYY-MM-DD"
          style={styles.dateInput}
          value={endDate}
        />
      </View>
      <AppText style={styles.hint}>{t('scope.rangeHint')}</AppText>

      <AppSwitch
        disabled
        onToggle={() => {}}
        text={t('scope.bleeding')}
        value
      />
      <AppSwitch
        disabled={Boolean(share) || isBusy}
        onToggle={(value) => updateScope('temperature', value)}
        text={t('scope.temperature')}
        value={scope.temperature}
      />
      <AppSwitch
        disabled={Boolean(share) || isBusy}
        onToggle={(value) => updateScope('symptoms', value)}
        text={t('scope.symptoms')}
        value={scope.symptoms}
      />
      <AppSwitch
        disabled={Boolean(share) || isBusy}
        onToggle={(value) => updateScope('fertilitySigns', value)}
        text={t('scope.fertilitySigns')}
        value={scope.fertilitySigns}
      />
      <AppSwitch
        disabled={Boolean(share) || isBusy}
        onToggle={(value) => updateScope('notes', value)}
        text={t('scope.notes')}
        value={scope.notes}
      />

      {!share && (
        <>
          <Button disabled={isBusy} onPress={previewData}>
            {t('previewButton')}
          </Button>
          {snapshot && (
            <>
              <Preview snapshot={snapshot} t={t} />
              {isBusy && (
                <AppText style={styles.status}>{t('working')}</AppText>
              )}
              <Button disabled={isBusy} isCTA onPress={createShare}>
                {t('button')}
              </Button>
            </>
          )}
        </>
      )}
      {share && (
        <View style={styles.share}>
          <SmartLinkMark />
          <View style={styles.qrBox}>
            <QRCode
              value={share.viewerLink}
              size={205}
              quietZone={6}
              backgroundColor="#ffffff"
            />
          </View>
          <AppText style={styles.status}>
            {t('status', {
              maxUses: share.maxUses,
              expiration: moment.unix(share.exp).format('YYYY-MM-DD'),
            })}
          </AppText>
          <Preview snapshot={snapshot} t={t} />
          {isBusy && <AppText style={styles.status}>{t('working')}</AppText>}
          <Button disabled={isBusy} onPress={copyLink}>
            {t('copyButton')}
          </Button>
          {hasCopied && <AppText>{t('copied')}</AppText>}
          <Button disabled={isBusy} onPress={shareLink}>
            {t('shareButton')}
          </Button>
          <Button disabled={isBusy} onPress={openViewer}>
            {t('openButton')}
          </Button>
          <Button disabled={isBusy} onPress={revokeShare}>
            {t('revokeButton')}
          </Button>
        </View>
      )}
    </Segment>
  )
}

const SmartLinkMark = () => (
  <View style={styles.smartMark}>
    <Svg width={25} height={20} viewBox="0 0 49 40">
      <Path d="M12.9297 0H18.2012L24.416 10.1238L30.7417 0H35.9022L24.416 18.652L12.9297 0Z" fill="#722772" />
      <Path d="M0 19.6422L2.66348 15.4607H15.0931L8.93377 5.22682L11.4863 0.990234L22.9171 19.6422H0Z" fill="#E24A31" />
      <Path d="M48.8858 21.293L46.2778 25.4745H33.8482L40.0075 35.8184L37.3995 40L25.9688 21.293H48.8858Z" fill="#89BF44" />
      <Path d="M37.3995 0.935547L40.063 5.22716L33.7927 15.461H46.3333L48.8858 19.6426H25.9688L37.3995 0.935547Z" fill="#E77D26" />
      <Path d="M11.4863 40L8.82279 35.7084L15.0931 25.4745H2.55251L0 21.293H22.9171L11.4863 40Z" fill="#F1B42A" />
    </Svg>
    <AppText style={styles.smartMarkText}>SMART Link</AppText>
  </View>
)

const Preview = ({ snapshot, t }) => {
  if (!snapshot) return null

  const { preview } = snapshot

  return (
    <View style={styles.preview}>
      <AppText style={styles.previewTitle}>{t('preview.title')}</AppText>
      <AppText>
        {t('preview.range', {
          startDate: snapshot.startDate,
          endDate: snapshot.endDate,
        })}
      </AppText>
      <AppText>
        {t('preview.counts', {
          dayCount: preview.dayCount,
          bleedingFacts: preview.menstrualBleedingFacts,
          nonMenstrualBleedingFacts: preview.nonMenstrualBleedingFacts,
          flowFacts: preview.flowFacts,
          temperatureFacts: preview.temperatureFacts,
          symptomFacts: preview.symptomFacts,
          fertilitySignFacts: preview.fertilitySignFacts,
          noteFacts: preview.noteFacts,
        })}
      </AppText>
      <AppText>{t('preview.access')}</AppText>
      <AppText>{t('preview.omissions')}</AppText>
    </View>
  )
}

ExportCycleFhirData.propTypes = {
  closePasswordConfirmation: PropTypes.func.isRequired,
}

Preview.propTypes = {
  snapshot: PropTypes.object,
  t: PropTypes.func.isRequired,
}

const styles = StyleSheet.create({
  dateInput: {
    flex: 1,
    marginRight: 8,
    minWidth: 0,
  },
  hint: {
    marginBottom: 8,
  },
  label: {
    marginTop: 16,
  },
  preview: {
    alignSelf: 'stretch',
    marginTop: 16,
  },
  previewTitle: {
    fontWeight: 'bold',
  },
  range: {
    flexDirection: 'row',
  },
  share: {
    alignItems: 'center',
    marginTop: 12,
  },
  qrBox: {
    backgroundColor: '#ffffff',
    borderColor: '#dddddd',
    borderRadius: 8,
    borderWidth: 1,
    marginTop: 4,
    padding: 8,
  },
  smartMark: {
    alignItems: 'center',
    flexDirection: 'row',
    marginBottom: 8,
  },
  smartMarkText: {
    fontSize: 18,
    fontWeight: 'bold',
    marginLeft: 6,
  },
  status: {
    marginTop: 16,
  },
})
