package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.enumerations.TutorialEventEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serial;

/**
 * Represents an entry of tutorial
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "tutorial_sections_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialSectionEntry implements EntityWithId<Long> {
    @Serial
    private static final long serialVersionUID = -2220422909343540317L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_num")
    private Integer order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_available_html_symbol_id")
    @Fetch(FetchMode.JOIN)
    private TutorialSectionAvailableHtmlSymbol htmlSymbol;

    @Enumerated(EnumType.STRING)
    private TutorialEventEnum event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "text_id")
    @Fetch(FetchMode.JOIN)
    private Translatable text;
}
