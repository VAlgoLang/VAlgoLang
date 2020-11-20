class Code_block:
    def __init__(self, code, syntax_highlighting=True, syntax_highlighting_style="inkpot", text_color=WHITE, text_weight=NORMAL, font="Times New Roman", tab_spacing=2):
        group = VGroup()

        if syntax_highlighting:
            fp = tempfile.NamedTemporaryFile(suffix='.re')

            for c in code:
                for sc in c:
                    fp.write(bytes(sc + "\n", encoding='utf-8'))

            fp.seek(0)

            self.paragraph = Code(fp.name, style=syntax_highlighting_style, language="reasonml", line_spacing=0.2,
                              tab_width=tab_spacing).code
            fp.close()
            group.add(self.paragraph)
            group.set_width(5)
            self.all = self.paragraph
        else:
            for c in code:
                for sc in c:
                    text = Text(sc, color=text_color, weight=text_weight, font=font)
                    group.add(text)
            group.set_width(5)
            self.all = group

        self.code = code


    def build(self):
        return self.all.arrange(DOWN, aligned_edge=LEFT, center=True)

    def get_line_at(self, line_number):
        idx = 0
        for i in range(line_number):
            idx += len(self.code[i])
        return self.all[idx-1]