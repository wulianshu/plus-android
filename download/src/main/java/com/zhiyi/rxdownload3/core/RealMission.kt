package com.zhiyi.rxdownload3.core

import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import com.zhiyi.rxdownload3.core.DownloadConfig.context
import com.zhiyi.rxdownload3.core.DownloadConfig.defaultSavePath
import com.zhiyi.rxdownload3.database.DbActor
import com.zhiyi.rxdownload3.extension.Extension
import com.zhiyi.rxdownload3.helper.*
import com.zhiyi.rxdownload3.http.HttpCore
import com.zhiyi.rxdownload3.notification.NotificationFactory
import com.zhiyicx.common.utils.FileUtils
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.schedulers.Schedulers.newThread
import retrofit2.Response
import java.io.File
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.SECONDS


class RealMission(val actual: Mission, private val semaphore: Semaphore,
                  private val autoStart: Boolean = false, initFlag: Boolean = true) {
    var totalSize = 0L

    var status: Status = Normal(Status())

    private var semaphoreFlag = false

    private val processor = BehaviorProcessor.create<Status>().toSerialized()

    private var disposable: Disposable? = null
    private var downloadType: DownloadType? = null

    private lateinit var downloadFlowable: Flowable<Status>

    private val enableNotification = DownloadConfig.enableNotification
    private val notificationPeriod = DownloadConfig.notificationPeriod
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationFactory: NotificationFactory

    private val enableDb = DownloadConfig.enableDb
    private lateinit var dbActor: DbActor

    private val extensions = mutableListOf<Extension>()

    init {
        if (initFlag) {
            init()

        }
    }

    private fun init() {
        Maybe.create<Any> {
            loadConfig()
            createFlowable()
            initMission()
            initExtension()
            initStatus()
            initNotification()

            it.onSuccess(ANY)
        }.subscribeOn(newThread()).doOnError {
            loge("init error!", it)
        }.subscribe {
            if (status is Succeed && getFile() == null) {
                if (enableDb) {
                    dbActor.delete(this)
                }
                status = Normal(Status())
            }
            emitStatus(status)
            if (autoStart || DownloadConfig.autoStart) {
                realStart()
            }
        }
    }

    private fun loadConfig() {
        if (enableNotification) {
            notificationManager = DownloadConfig.context!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationFactory = DownloadConfig.notificationFactory
        }

        if (enableDb) {
            dbActor = DownloadConfig.dbActor
        }
    }

    private fun initMission() {
        if (enableDb) {
            if (dbActor.isExists(this)) {
                dbActor.read(this)
            }
        }
    }

    private fun initExtension() {
        val ext = DownloadConfig.extensions
        ext.mapTo(extensions) { it.newInstance() }
        extensions.forEach { it.init(this) }
    }

    private fun initStatus() {
        downloadType = generateType()
        if (!enableDb) {
            downloadType?.initStatus()
        }
    }

    private fun initNotification() {
        processor.sample(notificationPeriod, SECONDS, true).subscribe {
            if (enableNotification) {
                val notification = notificationFactory.build(DownloadConfig.context!!, this, it)
                if (notification != null) {
                    notificationManager.notify(hashCode(), notification)
                }
            }
        }
    }


    private fun createFlowable() {
        downloadFlowable = Flowable.just(ANY)
                .subscribeOn(io())
                .doOnSubscribe {
                    emitStatusWithNotification(Waiting(status))
                    semaphoreFlag = false
                    semaphore.acquire()
                    semaphoreFlag = true
                }
                .subscribeOn(newThread())
                .flatMap { checkAndDownload() }
                .doOnError {
                    loge("Mission error! ${it.message}", it)
                    emitStatusWithNotification(Failed(status, it))
                }
                .doOnComplete {
                    logd("Mission complete!")
                    emitStatusWithNotification(Succeed(status))
                    FileUtils.updateMediaStore(context, getFile()?.absolutePath)
                    logd("Mission insertPhotoToAlbumAndRefresh!")
                }
                .doOnCancel {
                    logd("Mission cancel!")
                    emitStatusWithNotification(Suspend(status))
                }
                .doFinally {
                    logd("Mission finally!")
                    disposable = null
                    if (semaphoreFlag) {
                        semaphore.release()
                    }
                }
    }


    fun findExtension(extension: Class<out Extension>): Extension {
        return extensions.first { extension.isInstance(it) }
    }

    fun getFlowable(): Flowable<Status> {
        return processor
    }

    fun start(): Maybe<Any> {
        return Maybe.create<Any> {
            if (alreadyStarted()) {
                it.onSuccess(ANY)
                return@create
            }
            realStart()
            it.onSuccess(ANY)
        }.subscribeOn(newThread())
    }

    private fun alreadyStarted(): Boolean {
        if (status is Waiting || status is Downloading) {
            return true
        }
        return false
    }

    private fun realStart() {
        if (enableDb) {
            if (!dbActor.isExists(this)) {
                dbActor.create(this)
            }
        }

        if (disposable == null) {
            disposable = downloadFlowable.subscribe(this::emitStatusWithNotification)
        }
    }

    fun stop(): Maybe<Any> {
        return Maybe.create<Any> {
            realStop()
            it.onSuccess(ANY)
        }.subscribeOn(newThread())
    }

    internal fun realStop() {
        dispose(disposable)
        disposable = null
    }

    fun delete(deleteFile: Boolean): Maybe<Any> {
        return Maybe.create<Any> {
            //stop first.
            realStop()

            if (deleteFile) {
                downloadType?.delete()
            }

            if (enableDb) {
                dbActor.delete(this)
            }
            emitStatusWithNotification(Deleted(Status()))
            it.onSuccess(ANY)
        }.subscribeOn(newThread())
    }

    fun getFile(): File? {
        return downloadType?.getFile()
    }

    fun file(): Maybe<File> {
        return Maybe.create<File> {
            val file = getFile()
            if (file == null) {
                it.onError(RuntimeException("No such file"))
            } else {
                it.onSuccess(file)
            }
        }.subscribeOn(newThread())
    }

    fun setup(resp: Response<Void>) {
        actual.savePath = if (actual.savePath.isEmpty()) defaultSavePath else actual.savePath
        actual.saveName = fileName(actual.saveName, actual.url, resp)
        actual.rangeFlag = isSupportRange(resp)
        totalSize = contentLength(resp)

        downloadType = generateType()

        if (enableDb) {
            dbActor.update(this)
        }
    }

    fun emitStatusWithNotification(status: Status) {
        emitStatus(status)
    }

    fun emitStatus(status: Status) {
        this.status = status
        processor.onNext(status)
        if (enableDb) {
            dbActor.updateStatus(this)
        }
    }

    private fun generateType(): DownloadType? {
        return when {
            actual.rangeFlag == true -> RangeDownload(this)
            actual.rangeFlag == false -> NormalDownload(this)
            else -> null
        }
    }

    private fun checkAndDownload(): Flowable<Status> {
        return check().flatMapPublisher { download() }
    }

    private fun check(): Maybe<Any> {
        return if (actual.rangeFlag == null) {
            HttpCore.checkUrl(this)
        } else {
            Maybe.just(ANY)
        }
    }

    private fun download(): Flowable<out Status> {
        return downloadType?.download()
                ?: Flowable.error(IllegalStateException("Illegal download type"))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RealMission

        if (actual != other.actual) return false

        return true
    }

    override fun hashCode(): Int {
        return actual.hashCode()
    }
}