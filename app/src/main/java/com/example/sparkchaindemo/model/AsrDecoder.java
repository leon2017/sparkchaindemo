package com.example.sparkchaindemo.model;

public class AsrDecoder {

    private AsrResponseData.Text[] texts;
    private int defc = 10;

    public AsrDecoder() {
        this.texts = new AsrResponseData.Text[this.defc];
    }

    public synchronized void decode(AsrResponseData.Text text) {
        if (text.sn >= this.defc) {
            this.resize();
        }
        if ("rpl".equals(text.pgs)) {
            for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                this.texts[i].deleted = true;
            }
        }
        this.texts[text.sn] = text;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (AsrResponseData.Text t : this.texts) {
            if (t != null && !t.deleted) {
                sb.append(t.text);
            }
        }
        return sb.toString();
    }

    public void resize() {
        int oc = this.defc;
        this.defc <<= 1;
        AsrResponseData.Text[] old = this.texts;
        this.texts = new AsrResponseData.Text[this.defc];
        for (int i = 0; i < oc; i++) {
            this.texts[i] = old[i];
        }
    }

    public void discard() {
        for (int i = 0; i < this.texts.length; i++) {
            this.texts[i] = null;
        }
    }
}