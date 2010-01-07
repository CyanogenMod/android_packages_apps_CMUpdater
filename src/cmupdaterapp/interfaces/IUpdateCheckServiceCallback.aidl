package cmupdaterapp.interfaces;
import cmupdaterapp.customTypes.FullUpdateInfo;

interface IUpdateCheckServiceCallback
{
    void UpdateCheckFinished(in FullUpdateInfo fui);
}